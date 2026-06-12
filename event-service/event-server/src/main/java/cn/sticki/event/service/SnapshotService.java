package cn.sticki.event.service;

import cn.sticki.event.client.dto.HomepageVO;
import cn.sticki.event.entity.EventLedgerEntry;
import cn.sticki.event.entity.UserHomepageSnapshot;
import cn.sticki.event.mapper.EventLedgerMapper;
import cn.sticki.event.mapper.HomepageSnapshotMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SnapshotService {

	private static final int MAX_EVENTS_PER_REBUILD = 1000;
	private static final int MAX_ITEMS_PER_CATEGORY = 50;

	@Resource
	private EventLedgerMapper eventLedgerMapper;

	@Resource
	private HomepageSnapshotMapper homepageSnapshotMapper;

	@Resource
	private AccessTrendService accessTrendService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * 增量重建用户主页快照
	 */
	public void rebuildSnapshot(int userId) {
		// 1. 获取当前快照版本
		UserHomepageSnapshot existing = homepageSnapshotMapper.selectOne(
				new LambdaQueryWrapper<UserHomepageSnapshot>()
						.eq(UserHomepageSnapshot::getUserId, userId)
		);

		long currentVersion = existing != null ? existing.getSnapshotVersion() : 0;

		// 2. 查询增量事件
		List<EventLedgerEntry> events = eventLedgerMapper.selectByUserIdAfter(
				userId, currentVersion, MAX_EVENTS_PER_REBUILD
		);

		if (events.isEmpty() && existing != null) {
			log.debug("用户 {} 无新增事件，跳过重建", userId);
			return;
		}

		// 3. 加载现有快照数据或创建空快照
		List<Map<String, Object>> followDynamics = existing != null && existing.getFollowDynamics() != null
				? existing.getFollowDynamics() : new ArrayList<>();
		List<Map<String, Object>> blogInteractions = existing != null && existing.getBlogInteractions() != null
				? existing.getBlogInteractions() : new ArrayList<>();
		List<Map<String, Object>> commentReplies = existing != null && existing.getCommentReplies() != null
				? existing.getCommentReplies() : new ArrayList<>();
		List<Map<String, Object>> resourceUploads = existing != null && existing.getResourceUploads() != null
				? existing.getResourceUploads() : new ArrayList<>();

		// 4. 按时间顺序处理事件
		long maxId = currentVersion;
		for (EventLedgerEntry entry : events) {
			maxId = Math.max(maxId, entry.getId());
			processEvent(entry, followDynamics, blogInteractions, commentReplies, resourceUploads);
		}

		// 5. 裁剪列表防止膨胀
		trimList(followDynamics);
		trimList(blogInteractions);
		trimList(commentReplies);
		trimList(resourceUploads);

		// 6. 获取访问趋势
		Map<String, Object> accessTrends = accessTrendService.getAccessTrends(userId, 7);

		// 7. 计算汇总统计
		Map<String, Object> statsSummary = computeStatsSummary(
				followDynamics, blogInteractions, commentReplies, resourceUploads
		);

		// 8. 保存/更新快照
		UserHomepageSnapshot snapshot = existing != null ? existing : new UserHomepageSnapshot();
		snapshot.setUserId(userId);
		snapshot.setSnapshotVersion(maxId);
		snapshot.setFollowDynamics(followDynamics);
		snapshot.setBlogInteractions(blogInteractions);
		snapshot.setCommentReplies(commentReplies);
		snapshot.setResourceUploads(resourceUploads);
		snapshot.setAccessTrends(accessTrends);
		snapshot.setStatsSummary(statsSummary);

		if (existing != null) {
			homepageSnapshotMapper.updateById(snapshot);
		} else {
			homepageSnapshotMapper.insert(snapshot);
		}

		log.info("用户 {} 快照重建完成, version={}", userId, maxId);
	}

	/**
	 * 处理单条事件，更新聚合列表
	 */
	private void processEvent(EventLedgerEntry entry,
	                          List<Map<String, Object>> followDynamics,
	                          List<Map<String, Object>> blogInteractions,
	                          List<Map<String, Object>> commentReplies,
	                          List<Map<String, Object>> resourceUploads) {
		try {
			Map<String, Object> payload = objectMapper.readValue(
					entry.getEventPayload(), new TypeReference<>() {});
			String eventType = entry.getEventType();

			if (eventType.startsWith("user.follow") || eventType.startsWith("user.unfollow")) {
				Map<String, Object> item = new LinkedHashMap<>();
				item.put("targetUserId", payload.get("followId"));
				item.put("followed", payload.get("followed"));
				item.put("timestamp", entry.getTimestamp());
				// 移除旧的同一对用户关系记录（保证最新状态覆盖）
				Object fansId = payload.get("fansId");
				Object followId = payload.get("followId");
				followDynamics.removeIf(d ->
						Objects.equals(d.get("fansId"), fansId) && Objects.equals(d.get("followId"), followId));
				item.put("fansId", fansId);
				item.put("followId", followId);
				followDynamics.add(item);

			} else if (eventType.equals("blog.increase") || eventType.equals("blog.decrease")) {
				// 评论事件必须在通用 blog.* 之前匹配
				handleCommentEvent(entry, payload, commentReplies);

			} else if (eventType.startsWith("blog.")) {
				Map<String, Object> item = new LinkedHashMap<>();
				item.put("blogId", payload.get("blogId"));
				item.put("interactionType", eventType);
				item.put("actorUserId", payload.get("userId"));
				item.put("timestamp", entry.getTimestamp());
				// 对于同 idempotentKey 的重复事件，由于账本层已去重，这里直接添加
				blogInteractions.add(item);

			} else if (eventType.startsWith("resource.")) {
				Map<String, Object> item = new LinkedHashMap<>();
				item.put("resourceUrl", payload.get("resourceUrl"));
				item.put("resourceType", payload.get("resourceType"));
				item.put("timestamp", entry.getTimestamp());
				resourceUploads.add(item);
			}
		} catch (Exception e) {
			log.error("处理事件失败: id={}, type={}", entry.getId(), entry.getEventType(), e);
		}
	}

	/**
	 * 处理评论事件：increase 添加，decrease 回滚（移除对应评论）
	 */
	private void handleCommentEvent(EventLedgerEntry entry,
	                                Map<String, Object> payload,
	                                List<Map<String, Object>> commentReplies) {
		String eventType = entry.getEventType();
		if (eventType.equals("blog.increase")) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("commentId", payload.get("commentId"));
			item.put("blogId", payload.get("blogId"));
			item.put("userId", payload.get("userId"));
			item.put("content", payload.get("content"));
			item.put("timestamp", entry.getTimestamp());
			commentReplies.add(item);
		} else if (eventType.equals("blog.decrease")) {
			// 评论删除回滚：移除对应 commentId 的记录
			Object commentId = payload.get("commentId");
			if (commentId != null) {
				commentReplies.removeIf(c -> Objects.equals(c.get("commentId"), commentId));
			}
		}
	}

	private void trimList(List<Map<String, Object>> list) {
		while (list.size() > MAX_ITEMS_PER_CATEGORY) {
			list.remove(0); // 移除最旧的
		}
	}

	private Map<String, Object> computeStatsSummary(List<Map<String, Object>> followDynamics,
	                                                 List<Map<String, Object>> blogInteractions,
	                                                 List<Map<String, Object>> commentReplies,
	                                                 List<Map<String, Object>> resourceUploads) {
		Map<String, Object> stats = new LinkedHashMap<>();
		long followCount = followDynamics.stream()
				.filter(d -> Boolean.TRUE.equals(d.get("followed")))
				.count();
		stats.put("activeFollowCount", followCount);
		stats.put("blogInteractionCount", blogInteractions.size());
		stats.put("commentReplyCount", commentReplies.size());
		stats.put("resourceUploadCount", resourceUploads.size());
		return stats;
	}

	/**
	 * 获取用户快照，转为 HomepageVO
	 */
	public HomepageVO getSnapshot(int userId) {
		UserHomepageSnapshot snapshot = homepageSnapshotMapper.selectOne(
				new LambdaQueryWrapper<UserHomepageSnapshot>()
						.eq(UserHomepageSnapshot::getUserId, userId)
		);

		HomepageVO vo = new HomepageVO();
		vo.setUserId(userId);

		if (snapshot == null) {
			vo.setFollowDynamics(Collections.emptyList());
			vo.setBlogInteractions(Collections.emptyList());
			vo.setCommentReplies(Collections.emptyList());
			vo.setResourceUploads(Collections.emptyList());
			vo.setStatsSummary(Collections.emptyMap());
			return vo;
		}

		vo.setFollowDynamics(convertFollowDynamics(snapshot.getFollowDynamics()));
		vo.setBlogInteractions(convertBlogInteractions(snapshot.getBlogInteractions()));
		vo.setCommentReplies(convertCommentReplies(snapshot.getCommentReplies()));
		vo.setResourceUploads(convertResourceUploads(snapshot.getResourceUploads()));
		vo.setStatsSummary(snapshot.getStatsSummary());

		if (snapshot.getAccessTrends() != null) {
			HomepageVO.AccessTrendVO trendVO = new HomepageVO.AccessTrendVO();
			trendVO.setTotalPv(((Number) snapshot.getAccessTrends().getOrDefault("totalPv", 0)).longValue());
			trendVO.setTotalUv(((Number) snapshot.getAccessTrends().getOrDefault("totalUv", 0)).longValue());
			vo.setAccessTrends(trendVO);
		}

		return vo;
	}

	@SuppressWarnings("unchecked")
	private List<HomepageVO.FollowDynamicItem> convertFollowDynamics(List<Map<String, Object>> data) {
		if (data == null) return Collections.emptyList();
		return objectMapper.convertValue(data, new com.fasterxml.jackson.core.type.TypeReference<>() {});
	}

	@SuppressWarnings("unchecked")
	private List<HomepageVO.BlogInteractionItem> convertBlogInteractions(List<Map<String, Object>> data) {
		if (data == null) return Collections.emptyList();
		return objectMapper.convertValue(data, new com.fasterxml.jackson.core.type.TypeReference<>() {});
	}

	@SuppressWarnings("unchecked")
	private List<HomepageVO.CommentReplyItem> convertCommentReplies(List<Map<String, Object>> data) {
		if (data == null) return Collections.emptyList();
		return objectMapper.convertValue(data, new com.fasterxml.jackson.core.type.TypeReference<>() {});
	}

	@SuppressWarnings("unchecked")
	private List<HomepageVO.ResourceUploadItem> convertResourceUploads(List<Map<String, Object>> data) {
		if (data == null) return Collections.emptyList();
		return objectMapper.convertValue(data, new com.fasterxml.jackson.core.type.TypeReference<>() {});
	}
}

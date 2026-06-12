package cn.sticki.event.service;

import cn.sticki.event.client.dto.HomepageVO;
import cn.sticki.event.entity.EventLedgerEntry;
import cn.sticki.event.entity.UserHomepageSnapshot;
import cn.sticki.event.mapper.EventLedgerMapper;
import cn.sticki.event.mapper.HomepageSnapshotMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SnapshotService {

	private static final int MAX_EVENTS_PER_REBUILD = 1000;
	private static final int MAX_ITEMS_PER_CATEGORY = 50;

	private final EventLedgerMapper eventLedgerMapper;
	private final HomepageSnapshotMapper homepageSnapshotMapper;
	private final AccessTrendService accessTrendService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public SnapshotService(EventLedgerMapper eventLedgerMapper,
	                       HomepageSnapshotMapper homepageSnapshotMapper,
	                       AccessTrendService accessTrendService) {
		this.eventLedgerMapper = eventLedgerMapper;
		this.homepageSnapshotMapper = homepageSnapshotMapper;
		this.accessTrendService = accessTrendService;
	}

	public void rebuildSnapshot(int userId) {
		UserHomepageSnapshot snapshot = homepageSnapshotMapper.selectOne(
				new LambdaQueryWrapper<UserHomepageSnapshot>().eq(UserHomepageSnapshot::getUserId, userId)
		);

		boolean isNew = (snapshot == null);
		if (isNew) {
			snapshot = new UserHomepageSnapshot();
			snapshot.setUserId(userId);
			snapshot.setSnapshotVersion(0L);
			snapshot.setFollowDynamics(new ArrayList<>());
			snapshot.setBlogInteractions(new ArrayList<>());
			snapshot.setCommentReplies(new ArrayList<>());
			snapshot.setResourceUploads(new ArrayList<>());
			snapshot.setCreatedAt(LocalDateTime.now());
		}

		long afterId = snapshot.getSnapshotVersion();
		List<EventLedgerEntry> events = eventLedgerMapper.selectByUserIdAfter(userId, afterId, MAX_EVENTS_PER_REBUILD);

		long maxEventId = afterId;
		for (EventLedgerEntry entry : events) {
			processEvent(entry, snapshot);
			if (entry.getId() > maxEventId) {
				maxEventId = entry.getId();
			}
		}

		snapshot.setSnapshotVersion(maxEventId);

		// Trim lists to max items
		trimList(snapshot.getFollowDynamics());
		trimList(snapshot.getBlogInteractions());
		trimList(snapshot.getCommentReplies());
		trimList(snapshot.getResourceUploads());

		// Get access trends
		Map<String, Object> accessTrends = accessTrendService.getAccessTrends(userId, 7);
		snapshot.setAccessTrends(accessTrends);

		// Compute stats summary
		snapshot.setStatsSummary(computeStatsSummary(snapshot));
		snapshot.setUpdatedAt(LocalDateTime.now());

		if (isNew) {
			homepageSnapshotMapper.insert(snapshot);
		} else {
			homepageSnapshotMapper.updateById(snapshot);
		}
	}

	public HomepageVO getSnapshot(int userId) {
		UserHomepageSnapshot snapshot = homepageSnapshotMapper.selectOne(
				new LambdaQueryWrapper<UserHomepageSnapshot>().eq(UserHomepageSnapshot::getUserId, userId)
		);

		HomepageVO vo = new HomepageVO();
		vo.setUserId(userId);

		if (snapshot == null) {
			vo.setFollowDynamics(new ArrayList<>());
			vo.setBlogInteractions(new ArrayList<>());
			vo.setCommentReplies(new ArrayList<>());
			vo.setResourceUploads(new ArrayList<>());
			vo.setStatsSummary(new HashMap<>());
			return vo;
		}

		vo.setFollowDynamics(convertFollowDynamics(snapshot.getFollowDynamics()));
		vo.setBlogInteractions(convertBlogInteractions(snapshot.getBlogInteractions()));
		vo.setCommentReplies(convertCommentReplies(snapshot.getCommentReplies()));
		vo.setResourceUploads(convertResourceUploads(snapshot.getResourceUploads()));
		vo.setStatsSummary(snapshot.getStatsSummary());

		if (snapshot.getAccessTrends() != null) {
			HomepageVO.AccessTrendVO trends = new HomepageVO.AccessTrendVO();
			Object totalPv = snapshot.getAccessTrends().get("totalPv");
			Object totalUv = snapshot.getAccessTrends().get("totalUv");
			trends.setTotalPv(totalPv instanceof Number ? ((Number) totalPv).longValue() : 0);
			trends.setTotalUv(totalUv instanceof Number ? ((Number) totalUv).longValue() : 0);
			vo.setAccessTrends(trends);
		}

		return vo;
	}

	@SuppressWarnings("unchecked")
	private void processEvent(EventLedgerEntry entry, UserHomepageSnapshot snapshot) {
		String eventType = entry.getEventType();
		try {
			Map<String, Object> payload = objectMapper.readValue(entry.getEventPayload(), Map.class);

			switch (eventType) {
				case "blog.like", "blog.collect", "blog.like.cancel", "blog.collect.cancel",
						"blog.insert", "blog.delete", "blog.read", "blog.relay" -> {
					Map<String, Object> item = new LinkedHashMap<>();
					item.put("blogId", payload.get("blogId"));
					item.put("interactionType", eventType);
					item.put("actorUserId", payload.get("userId"));
					item.put("timestamp", entry.getTimestamp());
					snapshot.getBlogInteractions().add(item);
				}
				case "user.follow", "user.unfollow" -> handleFollowEvent(payload, entry, snapshot);
				case "blog.increase" -> handleCommentIncrease(payload, entry, snapshot);
				case "blog.decrease" -> handleCommentDecrease(payload, snapshot);
				case "resource.upload" -> {
					Map<String, Object> item = new LinkedHashMap<>();
					item.put("resourceUrl", payload.get("resourceUrl"));
					item.put("resourceType", payload.get("resourceType"));
					item.put("timestamp", entry.getTimestamp());
					snapshot.getResourceUploads().add(item);
				}
				default -> log.debug("Unknown event type: {}", eventType);
			}
		} catch (Exception e) {
			log.error("Failed to process event: id={}, type={}", entry.getId(), eventType, e);
		}
	}

	private void handleFollowEvent(Map<String, Object> payload, EventLedgerEntry entry, UserHomepageSnapshot snapshot) {
		Object fansIdObj = payload.get("fansId");
		Object followIdObj = payload.get("followId");
		Boolean followed = (Boolean) payload.get("followed");
		long timestamp = entry.getTimestamp();

		int fansId = fansIdObj instanceof Number ? ((Number) fansIdObj).intValue() : 0;
		int followId = followIdObj instanceof Number ? ((Number) followIdObj).intValue() : 0;

		// Find existing entry for same fansId+followId pair
		List<Map<String, Object>> dynamics = snapshot.getFollowDynamics();
		Optional<Map<String, Object>> existing = dynamics.stream()
				.filter(d -> {
					Object f = d.get("fansId");
					Object t = d.get("followId");
					int existFansId = f instanceof Number ? ((Number) f).intValue() : 0;
					int existFollowId = t instanceof Number ? ((Number) t).intValue() : 0;
					return existFansId == fansId && existFollowId == followId;
				})
				.findFirst();

		if (existing.isPresent()) {
			Map<String, Object> existingEntry = existing.get();
			Object existTs = existingEntry.get("timestamp");
			long existTimestamp = existTs instanceof Number ? ((Number) existTs).longValue() : 0;
			// Only update if new event has later timestamp
			if (timestamp > existTimestamp) {
				existingEntry.put("followed", followed);
				existingEntry.put("timestamp", timestamp);
			}
		} else {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("fansId", fansId);
			item.put("followId", followId);
			item.put("followed", followed);
			item.put("timestamp", timestamp);
			dynamics.add(item);
		}
	}

	private void handleCommentIncrease(Map<String, Object> payload, EventLedgerEntry entry, UserHomepageSnapshot snapshot) {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("commentId", payload.get("commentId"));
		item.put("blogId", payload.get("blogId"));
		item.put("userId", payload.get("userId"));
		item.put("content", payload.get("content"));
		item.put("timestamp", entry.getTimestamp());
		snapshot.getCommentReplies().add(item);
	}

	private void handleCommentDecrease(Map<String, Object> payload, UserHomepageSnapshot snapshot) {
		Object commentIdObj = payload.get("commentId");
		if (commentIdObj == null) return;
		int commentId = commentIdObj instanceof Number ? ((Number) commentIdObj).intValue() : 0;

		snapshot.getCommentReplies().removeIf(item -> {
			Object cid = item.get("commentId");
			int existId = cid instanceof Number ? ((Number) cid).intValue() : 0;
			return existId == commentId;
		});
	}

	private void trimList(List<?> list) {
		if (list != null && list.size() > MAX_ITEMS_PER_CATEGORY) {
			List<?> subList = new ArrayList<>(list.subList(list.size() - MAX_ITEMS_PER_CATEGORY, list.size()));
			list.clear();
			((List) list).addAll(subList);
		}
	}

	private Map<String, Object> computeStatsSummary(UserHomepageSnapshot snapshot) {
		Map<String, Object> stats = new LinkedHashMap<>();

		long activeFollowCount = snapshot.getFollowDynamics().stream()
				.filter(d -> Boolean.TRUE.equals(d.get("followed")))
				.count();
		stats.put("activeFollowCount", activeFollowCount);
		stats.put("blogInteractionCount", snapshot.getBlogInteractions().size());
		stats.put("commentReplyCount", snapshot.getCommentReplies().size());
		stats.put("resourceUploadCount", snapshot.getResourceUploads().size());

		return stats;
	}

	private List<HomepageVO.FollowDynamicItem> convertFollowDynamics(List<Map<String, Object>> data) {
		if (data == null) return new ArrayList<>();
		return data.stream().map(d -> {
			HomepageVO.FollowDynamicItem item = new HomepageVO.FollowDynamicItem();
			Object followId = d.get("followId");
			item.setTargetUserId(followId instanceof Number ? ((Number) followId).intValue() : null);
			item.setFollowed(Boolean.TRUE.equals(d.get("followed")));
			Object ts = d.get("timestamp");
			item.setTimestamp(ts instanceof Number ? ((Number) ts).longValue() : 0);
			return item;
		}).collect(Collectors.toList());
	}

	private List<HomepageVO.BlogInteractionItem> convertBlogInteractions(List<Map<String, Object>> data) {
		if (data == null) return new ArrayList<>();
		return data.stream().map(d -> {
			HomepageVO.BlogInteractionItem item = new HomepageVO.BlogInteractionItem();
			Object blogId = d.get("blogId");
			item.setBlogId(blogId instanceof Number ? ((Number) blogId).intValue() : null);
			item.setInteractionType((String) d.get("interactionType"));
			Object actor = d.get("actorUserId");
			item.setActorUserId(actor instanceof Number ? ((Number) actor).intValue() : null);
			Object ts = d.get("timestamp");
			item.setTimestamp(ts instanceof Number ? ((Number) ts).longValue() : 0);
			return item;
		}).collect(Collectors.toList());
	}

	private List<HomepageVO.CommentReplyItem> convertCommentReplies(List<Map<String, Object>> data) {
		if (data == null) return new ArrayList<>();
		return data.stream().map(d -> {
			HomepageVO.CommentReplyItem item = new HomepageVO.CommentReplyItem();
			Object cid = d.get("commentId");
			item.setCommentId(cid instanceof Number ? ((Number) cid).intValue() : null);
			Object bid = d.get("blogId");
			item.setBlogId(bid instanceof Number ? ((Number) bid).intValue() : null);
			Object uid = d.get("userId");
			item.setUserId(uid instanceof Number ? ((Number) uid).intValue() : null);
			item.setContent((String) d.get("content"));
			Object ts = d.get("timestamp");
			item.setTimestamp(ts instanceof Number ? ((Number) ts).longValue() : 0);
			return item;
		}).collect(Collectors.toList());
	}

	private List<HomepageVO.ResourceUploadItem> convertResourceUploads(List<Map<String, Object>> data) {
		if (data == null) return new ArrayList<>();
		return data.stream().map(d -> {
			HomepageVO.ResourceUploadItem item = new HomepageVO.ResourceUploadItem();
			item.setResourceUrl((String) d.get("resourceUrl"));
			item.setResourceType((String) d.get("resourceType"));
			Object ts = d.get("timestamp");
			item.setTimestamp(ts instanceof Number ? ((Number) ts).longValue() : 0);
			return item;
		}).collect(Collectors.toList());
	}

}

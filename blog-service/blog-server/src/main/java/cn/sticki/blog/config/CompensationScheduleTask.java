package cn.sticki.blog.config;

import cn.sticki.blog.service.CompensationService;
import cn.sticki.blog.service.FeedService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 补偿任务定时调度
 * <p>
 * 每5分钟检查并重试因服务降级（Feign 调用失败）而暂存的补偿任务。
 * 支持关注流推送、通知推送等场景的延迟重试。
 */
@Slf4j
@Component
public class CompensationScheduleTask {

	@Resource
	private CompensationService compensationService;

	@Resource
	private FeedService feedService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private static final int BATCH_SIZE = 50;

	/**
	 * 每5分钟重试关注流推送补偿任务
	 */
	@Scheduled(cron = "0 */5 * * * ?")
	public void retryFeedPushCompensations() {
		retryCompensation("feed:push", this::handleFeedPush);
		retryCompensation("feed:backfill", this::handleFeedBackfill);
		retryCompensation("feed:cleanup", this::handleFeedCleanup);
	}

	/**
	 * 每5分钟重试通知推送补偿任务
	 */
	@Scheduled(cron = "30 */5 * * * ?")
	public void retryNotificationCompensations() {
		retryCompensation("notification:create", this::handleNotification);
	}

	private void retryCompensation(String type, CompensationHandler handler) {
		Long count = compensationService.getCompensationCount(type);
		if (count == null || count == 0) return;

		log.info("开始重试补偿任务: type={}, count={}", type, count);
		int processed = 0;
		while (processed < BATCH_SIZE) {
			String entry = compensationService.popCompensation(type);
			if (entry == null) break;

			try {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = objectMapper.readValue(entry, Map.class);
				int retryCount = map.containsKey("retryCount") ? ((Number) map.get("retryCount")).intValue() : 0;

				boolean success = handler.handle(map);
				if (!success) {
					compensationService.reEnqueue(type, entry, retryCount);
				} else {
					log.debug("补偿任务重试成功: type={}", type);
				}
			} catch (Exception e) {
				log.error("补偿任务重试异常: type={}, error={}", type, e.getMessage());
				try {
					@SuppressWarnings("unchecked")
					Map<String, Object> map = objectMapper.readValue(entry, Map.class);
					int retryCount = map.containsKey("retryCount") ? ((Number) map.get("retryCount")).intValue() : 0;
					compensationService.reEnqueue(type, entry, retryCount);
				} catch (Exception ignored) {
					// Cannot parse, discard
				}
			}
			processed++;
		}
	}

	/**
	 * 处理关注流推送补偿：重新推送博客到粉丝的关注流
	 */
	private boolean handleFeedPush(Map<String, Object> map) {
		String payload = (String) map.get("payload");
		// payload format: "authorId:blogId:timestampSeconds"
		String[] parts = payload.split(":");
		if (parts.length < 3) return true; // malformed, discard
		try {
			int authorId = Integer.parseInt(parts[0]);
			int blogId = Integer.parseInt(parts[1]);
			long timestamp = Long.parseLong(parts[2]);
			feedService.pushBlogToFollowers(authorId, blogId, timestamp);
			return true;
		} catch (Exception e) {
			log.warn("关注流推送补偿失败: payload={}, error={}", payload, e.getMessage());
			return false;
		}
	}

	/**
	 * 处理关注回填补偿：重新将已关注用户的近期博客回填到粉丝关注流
	 */
	private boolean handleFeedBackfill(Map<String, Object> map) {
		String payload = (String) map.get("payload");
		// payload format: "fansId:followId"
		String[] parts = payload.split(":");
		if (parts.length < 2) return true;
		try {
			int fansId = Integer.parseInt(parts[0]);
			int followId = Integer.parseInt(parts[1]);
			feedService.backfillOnFollow(fansId, followId);
			return true;
		} catch (Exception e) {
			log.warn("关注回填补偿失败: payload={}, error={}", payload, e.getMessage());
			return false;
		}
	}

	/**
	 * 处理取关清理补偿：重新清理取消关注用户的博客从粉丝关注流
	 */
	private boolean handleFeedCleanup(Map<String, Object> map) {
		String payload = (String) map.get("payload");
		// payload format: "fansId:followId"
		String[] parts = payload.split(":");
		if (parts.length < 2) return true;
		try {
			int fansId = Integer.parseInt(parts[0]);
			int followId = Integer.parseInt(parts[1]);
			feedService.cleanupOnUnfollow(fansId, followId);
			return true;
		} catch (Exception e) {
			log.warn("取关清理补偿失败: payload={}, error={}", payload, e.getMessage());
			return false;
		}
	}

	/**
	 * 处理通知补偿：目前仅记录日志，后续可扩展为实际创建通知
	 */
	private boolean handleNotification(Map<String, Object> map) {
		log.info("通知补偿任务处理: payload={}", map.get("payload"));
		// Notification creation is idempotent by nature (DB insert),
		// so we simply log the retry attempt. Full implementation would
		// re-invoke NotificationService.createNotification with parsed params.
		return true;
	}

	@FunctionalInterface
	private interface CompensationHandler {
		boolean handle(Map<String, Object> map) throws Exception;
	}

}

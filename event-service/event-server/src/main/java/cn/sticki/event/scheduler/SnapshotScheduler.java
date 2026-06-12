package cn.sticki.event.scheduler;

import cn.sticki.event.service.SnapshotService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
public class SnapshotScheduler {

	private static final String DIRTY_SET_KEY = "snapshot:dirty";

	@Resource
	private SnapshotService snapshotService;

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	/**
	 * 每 30 分钟重建脏用户快照
	 */
	@Scheduled(fixedRate = 1800000)
	public void rebuildDirtySnapshots() {
		Set<String> dirtyUserIds = stringRedisTemplate.opsForSet().members(DIRTY_SET_KEY);
		if (dirtyUserIds == null || dirtyUserIds.isEmpty()) {
			return;
		}

		log.info("开始重建 {} 个脏用户的快照", dirtyUserIds.size());
		int successCount = 0;
		int failCount = 0;

		for (String userIdStr : dirtyUserIds) {
			try {
				int userId = Integer.parseInt(userIdStr);
				snapshotService.rebuildSnapshot(userId);
				stringRedisTemplate.opsForSet().remove(DIRTY_SET_KEY, userIdStr);
				successCount++;
			} catch (NumberFormatException e) {
				log.warn("脏用户 ID 格式错误: {}", userIdStr);
				stringRedisTemplate.opsForSet().remove(DIRTY_SET_KEY, userIdStr);
			} catch (Exception e) {
				log.error("快照重建失败: userId={}", userIdStr, e);
				failCount++;
			}
		}

		log.info("快照重建完成: 成功={}, 失败={}", successCount, failCount);
	}
}

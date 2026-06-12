package cn.sticki.event.scheduler;

import cn.sticki.event.service.SnapshotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SnapshotScheduler {

	private static final String DIRTY_SET_KEY = "snapshot:dirty";

	private final SnapshotService snapshotService;
	private final StringRedisTemplate stringRedisTemplate;

	public SnapshotScheduler(SnapshotService snapshotService, StringRedisTemplate stringRedisTemplate) {
		this.snapshotService = snapshotService;
		this.stringRedisTemplate = stringRedisTemplate;
	}

	@Scheduled(fixedRate = 60000)
	public void rebuildDirtySnapshots() {
		String userId;
		while ((userId = stringRedisTemplate.opsForSet().pop(DIRTY_SET_KEY)) != null) {
			try {
				snapshotService.rebuildSnapshot(Integer.parseInt(userId));
			} catch (Exception e) {
				log.error("Failed to rebuild snapshot for userId={}", userId, e);
			}
		}
	}

}

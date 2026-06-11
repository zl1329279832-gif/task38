package cn.sticki.blog.config;

import cn.sticki.blog.service.RankService;
import cn.sticki.blog.utils.RankKeyUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

import static cn.sticki.blog.constants.RedisConstants.RANK_HOT_REALTIME_KEY;

@Slf4j
@Component
@EnableScheduling
public class RankScheduleTask {

	@Resource
	private RankService rankService;

	@Resource
	private RedisTemplate<String, Integer> redisTemplate;

	/**
	 * 每10分钟重新计算热榜分数（应用时间衰减）
	 */
	@Scheduled(cron = "0 */10 * * * ?")
	public void recalculateHotScores() {
		log.debug("定时重算热榜分数开始");
		long weekKey = RankKeyUtils.getWeekKey();
		String realtimeKey = RANK_HOT_REALTIME_KEY + weekKey;
		Set<ZSetOperations.TypedTuple<Integer>> tuples = redisTemplate.opsForZSet().rangeWithScores(realtimeKey, 0, -1);
		if (tuples == null) return;
		for (ZSetOperations.TypedTuple<Integer> tuple : tuples) {
			Integer blogId = tuple.getValue();
			if (blogId != null) {
				try {
					rankService.recalculateBlogHotScore(blogId);
				} catch (Exception e) {
					log.error("重算博客{}热榜分数失败: {}", blogId, e.getMessage());
				}
			}
		}
		log.debug("定时重算热榜分数完成, 共{}篇博客", tuples.size());
	}

}

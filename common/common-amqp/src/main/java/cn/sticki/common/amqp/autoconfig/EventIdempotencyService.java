package cn.sticki.common.amqp.autoconfig;

import cn.sticki.common.amqp.event.BaseEvent;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的事件幂等消费服务。
 * 使用 SET-IF-ABSENT + 时间戳比较，保证同一幂等键的事件只被成功处理一次，
 * 且旧事件不能覆盖新状态。
 */
@Service
public class EventIdempotencyService {

	private static final String KEY_PREFIX = "event:processed:";
	private static final long TTL_HOURS = 24;

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	/**
	 * 尝试消费事件。
	 * <p>
	 * 逻辑：
	 * 1. 以幂等键为 Redis key，尝试 SET-IF-ABSENT 写入时间戳
	 * 2. 若写入成功，说明是首次消费，返回 true
	 * 3. 若写入失败，比较当前事件时间戳与已存储时间戳：
	 *    - 已存储值为空（key 已过期）→ 接受
	 *    - 当前事件更新 → 用 SET 覆盖，接受
	 *    - 当前事件更旧 → 拒绝
	 *
	 * @param event 待消费事件
	 * @return true 表示可以消费，false 表示重复或过时事件
	 */
	public boolean tryConsume(BaseEvent event) {
		String key = KEY_PREFIX + event.getIdempotentKey();
		String timestampStr = String.valueOf(event.getTimestamp());

		Boolean absent = stringRedisTemplate.opsForValue()
				.setIfAbsent(key, timestampStr, TTL_HOURS, TimeUnit.HOURS);

		if (Boolean.TRUE.equals(absent)) {
			return true;
		}

		// key 已存在，比较时间戳
		String stored = stringRedisTemplate.opsForValue().get(key);
		if (stored == null) {
			// key 在 setIfAbsent 和 get 之间过期了，接受事件
			return true;
		}

		long storedTimestamp = Long.parseLong(stored);
		if (event.getTimestamp() > storedTimestamp) {
			// 当前事件更新，覆盖写入
			stringRedisTemplate.opsForValue().set(key, timestampStr, TTL_HOURS, TimeUnit.HOURS);
			return true;
		}

		// 旧事件，拒绝
		return false;
	}

}

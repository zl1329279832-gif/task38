package cn.sticki.common.amqp.autoconfig;

import cn.sticki.common.amqp.event.BaseEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 事件幂等性服务，基于 Redis SET NX + 时间戳比较
 */
@Slf4j
@Component
public class EventIdempotencyService {

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	private static final String IDEMPOTENT_PREFIX = "event:processed:";
	private static final long IDEMPOTENT_TTL_SECONDS = 86400;

	/**
	 * 尝试消费事件。
	 * 首次或更新事件返回true，重复/过期事件返回false。
	 */
	public boolean tryConsume(BaseEvent event) {
		String key = IDEMPOTENT_PREFIX + event.getIdempotentKey();
		String tsStr = String.valueOf(event.getTimestamp());
		Boolean isFirst = stringRedisTemplate.opsForValue()
				.setIfAbsent(key, tsStr, IDEMPOTENT_TTL_SECONDS, TimeUnit.SECONDS);
		if (Boolean.TRUE.equals(isFirst)) {
			return true;
		}
		String stored = stringRedisTemplate.opsForValue().get(key);
		if (stored != null && Long.parseLong(stored) >= event.getTimestamp()) {
			log.warn("过期/重复事件被拒绝: type={}, id={}, key={}",
					event.getEventType(), event.getEventId(), event.getIdempotentKey());
			return false;
		}
		stringRedisTemplate.opsForValue().set(key, tsStr, IDEMPOTENT_TTL_SECONDS, TimeUnit.SECONDS);
		return true;
	}
}

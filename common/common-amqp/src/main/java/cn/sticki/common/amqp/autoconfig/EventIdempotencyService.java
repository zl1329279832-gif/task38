package cn.sticki.common.amqp.autoconfig;

import cn.sticki.common.amqp.event.BaseEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 事件幂等性服务，基于 Redis Lua 脚本原子比较时间戳
 */
@Slf4j
@Component
public class EventIdempotencyService {

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	private static final String IDEMPOTENT_PREFIX = "event:processed:";
	private static final long IDEMPOTENT_TTL_SECONDS = 86400;

	/**
	 * Lua脚本：原子性地比较并设置事件时间戳。
	 * KEYS[1] = 幂等键, ARGV[1] = 事件时间戳, ARGV[2] = TTL秒数
	 * 返回 1 表示接受（首次或更新），0 表示拒绝（重复/过期）
	 */
	private static final DefaultRedisScript<Long> IDEMPOTENT_SCRIPT;

	static {
		IDEMPOTENT_SCRIPT = new DefaultRedisScript<>();
		IDEMPOTENT_SCRIPT.setScriptText(
				"local stored = redis.call('GET', KEYS[1]) " +
				"if stored == false then " +
				"  redis.call('SET', KEYS[1], ARGV[1], 'EX', tonumber(ARGV[2])) " +
				"  return 1 " +
				"end " +
				"if tonumber(stored) >= tonumber(ARGV[1]) then " +
				"  return 0 " +
				"end " +
				"redis.call('SET', KEYS[1], ARGV[1], 'EX', tonumber(ARGV[2])) " +
				"return 1"
		);
		IDEMPOTENT_SCRIPT.setResultType(Long.class);
	}

	/**
	 * 尝试消费事件。
	 * 首次或更新事件返回true，重复/过期事件返回false。
	 * 使用 Lua 脚本保证原子性，避免并发竞态。
	 */
	public boolean tryConsume(BaseEvent event) {
		String key = IDEMPOTENT_PREFIX + event.getIdempotentKey();
		String tsStr = String.valueOf(event.getTimestamp());
		Long result = stringRedisTemplate.execute(
				IDEMPOTENT_SCRIPT,
				List.of(key),
				tsStr,
				String.valueOf(IDEMPOTENT_TTL_SECONDS)
		);
		if (result == null || result == 0L) {
			log.warn("过期/重复事件被拒绝: type={}, id={}, key={}",
					event.getEventType(), event.getEventId(), event.getIdempotentKey());
			return false;
		}
		return true;
	}
}

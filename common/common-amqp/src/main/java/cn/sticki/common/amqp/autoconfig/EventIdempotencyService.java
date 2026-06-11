package cn.sticki.common.amqp.autoconfig;

import cn.sticki.common.amqp.event.BaseEvent;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 事件幂等性服务，基于 Redis Lua 脚本实现原子性时间戳比较
 * <p>
 * 使用 Lua 脚本保证 GET + COMPARE + SET 的原子性，避免并发场景下
 * 旧事件覆盖新事件的竞态条件。
 */
@Slf4j
@Component
public class EventIdempotencyService {

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	private static final String IDEMPOTENT_PREFIX = "event:processed:";
	private static final long IDEMPOTENT_TTL_SECONDS = 86400;

	/**
	 * Lua 脚本：原子性地比较并更新时间戳
	 * <p>
	 * 逻辑：
	 * - key 不存在：写入新时间戳，返回 1（接受）
	 * - key 存在且已存储时间戳 < 新时间戳：更新时间戳，返回 1（接受更新版本）
	 * - key 存在且已存储时间戳 >= 新时间戳：返回 0（拒绝过期/重复事件）
	 */
	private static final DefaultRedisScript<Long> COMPARE_AND_SET_SCRIPT;

	static {
		COMPARE_AND_SET_SCRIPT = new DefaultRedisScript<>();
		COMPARE_AND_SET_SCRIPT.setScriptText(
				"local key = KEYS[1]\n" +
				"local newTs = tonumber(ARGV[1])\n" +
				"local ttl = tonumber(ARGV[2])\n" +
				"local stored = redis.call('get', key)\n" +
				"if stored == false then\n" +
				"    redis.call('set', key, newTs, 'EX', ttl)\n" +
				"    return 1\n" +
				"end\n" +
				"if tonumber(stored) < newTs then\n" +
				"    redis.call('set', key, newTs, 'EX', ttl)\n" +
				"    return 1\n" +
				"end\n" +
				"return 0"
		);
		COMPARE_AND_SET_SCRIPT.setResultType(Long.class);
	}

	/**
	 * 尝试消费事件。
	 * <p>
	 * 首次或更新事件返回true，重复/过期事件返回false。
	 * 使用 Lua 脚本保证原子性，避免并发竞态条件。
	 */
	public boolean tryConsume(BaseEvent event) {
		String key = IDEMPOTENT_PREFIX + event.getIdempotentKey();
		String tsStr = String.valueOf(event.getTimestamp());
		Long result = stringRedisTemplate.execute(
				COMPARE_AND_SET_SCRIPT,
				Collections.singletonList(key),
				tsStr,
				String.valueOf(IDEMPOTENT_TTL_SECONDS)
		);
		if (result != null && result == 1L) {
			return true;
		}
		log.warn("过期/重复事件被拒绝: type={}, id={}, key={}",
				event.getEventType(), event.getEventId(), event.getIdempotentKey());
		return false;
	}
}

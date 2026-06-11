package cn.sticki.blog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 服务降级补偿服务
 * <p>
 * 当 Feign 调用失败（如下游服务不可用）时，将失败的操作序列化后存入 Redis 列表，
 * 由定时任务定期重试，确保最终一致性。
 * <p>
 * 补偿任务存储结构：Redis List，key = "compensation:{type}"，每个元素为 JSON 格式：
 * {"payload":"...", "timestamp":1234567890, "retryCount":0}
 */
@Slf4j
@Service
public class CompensationService {

	private static final String COMPENSATION_KEY_PREFIX = "compensation:";
	private static final long COMPENSATION_TTL_SECONDS = 60 * 60 * 24 * 7L; // 7 days
	private static final int MAX_RETRY_COUNT = 5;

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * 保存补偿任务
	 *
	 * @param type      补偿类型，如 "feed:push"、"notification:create"
	 * @param payload   任务数据（JSON 或简单标识）
	 * @param timestamp 事件原始时间戳，用于重试时的幂等判断
	 */
	public void saveCompensation(String type, String payload, long timestamp) {
		try {
			String entry = objectMapper.writeValueAsString(Map.of(
					"payload", payload,
					"timestamp", timestamp,
					"retryCount", 0
			));
			String key = COMPENSATION_KEY_PREFIX + type;
			stringRedisTemplate.opsForList().rightPush(key, entry);
			stringRedisTemplate.expire(key, COMPENSATION_TTL_SECONDS, TimeUnit.SECONDS);
			log.info("补偿任务已保存: type={}, payload={}", type, payload);
		} catch (Exception e) {
			log.error("保存补偿任务失败: type={}, payload={}, error={}", type, payload, e.getMessage());
		}
	}

	/**
	 * 弹出一条补偿任务（FIFO）
	 *
	 * @param type 补偿类型
	 * @return 补偿任务 JSON 字符串，无任务时返回 null
	 */
	public String popCompensation(String type) {
		String key = COMPENSATION_KEY_PREFIX + type;
		return stringRedisTemplate.opsForList().leftPop(key);
	}

	/**
	 * 将重试失败的任务重新入队
	 *
	 * @param type       补偿类型
	 * @param entry      原始任务 JSON
	 * @param retryCount 当前重试次数
	 */
	public void reEnqueue(String type, String entry, int retryCount) {
		if (retryCount >= MAX_RETRY_COUNT) {
			log.error("补偿任务达到最大重试次数，丢弃: type={}, entry={}", type, entry);
			return;
		}
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = objectMapper.readValue(entry, Map.class);
			map.put("retryCount", retryCount + 1);
			String updatedEntry = objectMapper.writeValueAsString(map);
			String key = COMPENSATION_KEY_PREFIX + type;
			stringRedisTemplate.opsForList().rightPush(key, updatedEntry);
		} catch (Exception e) {
			log.error("重新入队补偿任务失败: type={}, error={}", type, e.getMessage());
		}
	}

	/**
	 * 获取指定类型的补偿任务数量
	 */
	public Long getCompensationCount(String type) {
		return stringRedisTemplate.opsForList().size(COMPENSATION_KEY_PREFIX + type);
	}

}

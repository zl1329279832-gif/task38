package cn.sticki.common.amqp.compensation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis Hash 的补偿任务存储
 * <p>
 * Key: compensation:tasks:{consumerName}
 * Field: idempotentKey
 * Value: CompensationTask JSON
 * TTL: 7天
 */
@Slf4j
@Component
public class CompensationTaskRepository {

	private static final String KEY_PREFIX = "compensation:tasks:";
	private static final long TTL_SECONDS = 60 * 60 * 24 * 7L; // 7 days

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * 保存补偿任务
	 */
	public void save(String consumerName, CompensationTask task) {
		String key = KEY_PREFIX + consumerName;
		try {
			String json = objectMapper.writeValueAsString(task);
			stringRedisTemplate.opsForHash().put(key, task.getIdempotentKey(), json);
			stringRedisTemplate.expire(key, TTL_SECONDS, TimeUnit.SECONDS);
		} catch (JsonProcessingException e) {
			log.error("序列化补偿任务失败: {}", e.getMessage());
		}
	}

	/**
	 * 获取指定消费者的所有待处理任务
	 */
	public List<CompensationTask> findPendingTasks(String consumerName) {
		String key = KEY_PREFIX + consumerName;
		Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
		List<CompensationTask> tasks = new ArrayList<>();
		for (Map.Entry<Object, Object> entry : entries.entrySet()) {
			try {
				CompensationTask task = objectMapper.readValue((String) entry.getValue(), CompensationTask.class);
				if (CompensationTask.STATUS_PENDING.equals(task.getStatus())) {
					tasks.add(task);
				}
			} catch (JsonProcessingException e) {
				log.warn("反序列化补偿任务失败: key={}, error={}", entry.getKey(), e.getMessage());
			}
		}
		return tasks;
	}

	/**
	 * 删除已完成的补偿任务
	 */
	public void remove(String consumerName, String idempotentKey) {
		String key = KEY_PREFIX + consumerName;
		stringRedisTemplate.opsForHash().delete(key, idempotentKey);
	}

	/**
	 * 更新补偿任务（重试后更新状态）
	 */
	public void update(String consumerName, CompensationTask task) {
		save(consumerName, task);
	}

	/**
	 * 清理已完成或已过期的任务
	 */
	public void cleanupCompleted(String consumerName) {
		String key = KEY_PREFIX + consumerName;
		Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
		for (Map.Entry<Object, Object> entry : entries.entrySet()) {
			try {
				CompensationTask task = objectMapper.readValue((String) entry.getValue(), CompensationTask.class);
				if (CompensationTask.STATUS_COMPLETED.equals(task.getStatus())
						|| CompensationTask.STATUS_FAILED.equals(task.getStatus())) {
					stringRedisTemplate.opsForHash().delete(key, entry.getKey());
				}
			} catch (JsonProcessingException e) {
				// 反序列化失败的任务也清理掉
				stringRedisTemplate.opsForHash().delete(key, entry.getKey());
			}
		}
	}
}

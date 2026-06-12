package cn.sticki.common.amqp.compensation;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
public class CompensationTaskRepository {

	private static final String KEY_PREFIX = "compensation:tasks:";

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public CompensationTaskRepository(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	public void save(String consumer, CompensationTask task) {
		try {
			String json = objectMapper.writeValueAsString(task);
			stringRedisTemplate.opsForHash().put(KEY_PREFIX + consumer, task.getIdempotentKey(), json);
		} catch (Exception e) {
			log.error("Failed to save compensation task for consumer={}, key={}", consumer, task.getIdempotentKey(), e);
		}
	}

	public Map<String, CompensationTask> findPendingTasks(String consumer) {
		Map<String, CompensationTask> result = new LinkedHashMap<>();
		Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(KEY_PREFIX + consumer);
		for (Map.Entry<Object, Object> entry : entries.entrySet()) {
			try {
				CompensationTask task = objectMapper.readValue((String) entry.getValue(), CompensationTask.class);
				if (CompensationTask.STATUS_PENDING.equals(task.getStatus())) {
					result.put((String) entry.getKey(), task);
				}
			} catch (Exception e) {
				log.error("Failed to deserialize compensation task: key={}", entry.getKey(), e);
			}
		}
		return result;
	}

	public void remove(String consumer, String key) {
		stringRedisTemplate.opsForHash().delete(KEY_PREFIX + consumer, key);
	}

	public void update(String consumer, CompensationTask task) {
		save(consumer, task);
	}

	public void cleanupCompleted(String consumer) {
		Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(KEY_PREFIX + consumer);
		for (Map.Entry<Object, Object> entry : entries.entrySet()) {
			try {
				CompensationTask task = objectMapper.readValue((String) entry.getValue(), CompensationTask.class);
				if (CompensationTask.STATUS_COMPLETED.equals(task.getStatus())
						|| CompensationTask.STATUS_FAILED.equals(task.getStatus())) {
					stringRedisTemplate.opsForHash().delete(KEY_PREFIX + consumer, entry.getKey());
				}
			} catch (Exception e) {
				log.error("Failed to parse task during cleanup: key={}", entry.getKey(), e);
			}
		}
	}

}

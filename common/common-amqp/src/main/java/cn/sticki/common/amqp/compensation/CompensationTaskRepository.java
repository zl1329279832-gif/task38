package cn.sticki.common.amqp.compensation;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class CompensationTaskRepository {

	private static final String KEY_PREFIX = "compensation:tasks:";

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public void save(String consumerName, CompensationTask task) {
		HashOperations<String, Object, Object> ops = stringRedisTemplate.opsForHash();
		try {
			ops.put(KEY_PREFIX + consumerName, task.getIdempotentKey(), objectMapper.writeValueAsString(task));
		} catch (Exception ignored) {
		}
	}

	public List<CompensationTask> findPendingTasks(String consumerName) {
		HashOperations<String, Object, Object> ops = stringRedisTemplate.opsForHash();
		Map<Object, Object> entries = ops.entries(KEY_PREFIX + consumerName);
		List<CompensationTask> tasks = new ArrayList<>();
		for (Object value : entries.values()) {
			try {
				CompensationTask task = objectMapper.readValue((String) value, CompensationTask.class);
				if (CompensationTask.STATUS_PENDING.equals(task.getStatus())) {
					tasks.add(task);
				}
			} catch (Exception ignored) {
			}
		}
		return tasks;
	}

	public void remove(String consumerName, String idempotentKey) {
		stringRedisTemplate.opsForHash().delete(KEY_PREFIX + consumerName, idempotentKey);
	}

	public void update(String consumerName, CompensationTask task) {
		save(consumerName, task);
	}

	public void cleanupCompleted(String consumerName) {
		HashOperations<String, Object, Object> ops = stringRedisTemplate.opsForHash();
		Map<Object, Object> entries = ops.entries(KEY_PREFIX + consumerName);
		for (Map.Entry<Object, Object> entry : entries.entrySet()) {
			try {
				CompensationTask task = objectMapper.readValue((String) entry.getValue(), CompensationTask.class);
				if (CompensationTask.STATUS_COMPLETED.equals(task.getStatus())
						|| CompensationTask.STATUS_FAILED.equals(task.getStatus())) {
					ops.delete(KEY_PREFIX + consumerName, entry.getKey());
				}
			} catch (Exception ignored) {
			}
		}
	}
}

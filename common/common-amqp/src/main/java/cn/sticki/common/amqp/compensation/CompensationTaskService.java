package cn.sticki.common.amqp.compensation;

import cn.sticki.common.amqp.event.BaseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
public class CompensationTaskService {

	private static final int DEFAULT_MAX_RETRIES = 3;

	private final CompensationTaskRepository compensationTaskRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public CompensationTaskService(CompensationTaskRepository compensationTaskRepository) {
		this.compensationTaskRepository = compensationTaskRepository;
	}

	public void saveForRetry(BaseEvent event, String consumer, Exception error) {
		try {
			CompensationTask task = new CompensationTask();
			task.setEventType(event.getEventType());
			task.setEventPayload(objectMapper.writeValueAsString(event));
			task.setEventClassName(event.getClass().getName());
			task.setIdempotentKey(event.getIdempotentKey());
			task.setStatus(CompensationTask.STATUS_PENDING);
			task.setRetryCount(0);
			task.setMaxRetries(DEFAULT_MAX_RETRIES);
			task.setCreateTime(System.currentTimeMillis());
			task.setNextRetryTime(0);
			task.setErrorMessage(error.getMessage());

			compensationTaskRepository.save(consumer, task);
		} catch (Exception e) {
			log.error("Failed to save compensation task for consumer={}", consumer, e);
		}
	}

	public <T extends BaseEvent> void replayPendingTasks(String consumer, Consumer<T> handler, Class<T> eventClass) {
		Map<String, CompensationTask> pendingTasks = compensationTaskRepository.findPendingTasks(consumer);

		for (Map.Entry<String, CompensationTask> entry : pendingTasks.entrySet()) {
			String key = entry.getKey();
			CompensationTask task = entry.getValue();

			if (task.getRetryCount() >= task.getMaxRetries()) {
				task.setStatus(CompensationTask.STATUS_FAILED);
				compensationTaskRepository.update(consumer, task);
				log.warn("Compensation task exceeded max retries: consumer={}, key={}", consumer, key);
				continue;
			}

			try {
				T event = objectMapper.readValue(task.getEventPayload(), eventClass);
				handler.accept(event);
				compensationTaskRepository.remove(consumer, key);
			} catch (Exception e) {
				log.error("Compensation replay failed: consumer={}, key={}, retryCount={}",
						consumer, key, task.getRetryCount(), e);
				task.setRetryCount(task.getRetryCount() + 1);
				task.setErrorMessage(e.getMessage());
				task.setNextRetryTime(System.currentTimeMillis() + 60000L);
				compensationTaskRepository.update(consumer, task);
			}
		}
	}

	public void cleanupCompleted(String consumer) {
		compensationTaskRepository.cleanupCompleted(consumer);
	}

}

package cn.sticki.common.amqp.compensation;

import cn.sticki.common.amqp.event.BaseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Component
public class CompensationTaskService {

	private static final int DEFAULT_MAX_RETRIES = 3;

	@Resource
	private CompensationTaskRepository compensationTaskRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public void saveForRetry(BaseEvent event, String consumerName, Exception error) {
		CompensationTask task = new CompensationTask();
		task.setEventType(event.getEventType());
		task.setIdempotentKey(event.getIdempotentKey());
		task.setStatus(CompensationTask.STATUS_PENDING);
		task.setRetryCount(0);
		task.setMaxRetries(DEFAULT_MAX_RETRIES);
		task.setCreateTime(System.currentTimeMillis());
		task.setNextRetryTime(0);
		task.setErrorMessage(error.getMessage());
		try {
			task.setEventPayload(objectMapper.writeValueAsString(event));
			task.setEventClassName(event.getClass().getName());
		} catch (Exception e) {
			log.error("序列化补偿事件失败", e);
			return;
		}
		compensationTaskRepository.save(consumerName, task);
		log.info("补偿任务已保存: consumer={}, key={}", consumerName, event.getIdempotentKey());
	}

	@SuppressWarnings("unchecked")
	public <T extends BaseEvent> void replayPendingTasks(String consumerName, Consumer<T> handler, Class<T> eventClass) {
		List<CompensationTask> tasks = compensationTaskRepository.findPendingTasks(consumerName);
		for (CompensationTask task : tasks) {
			if (task.getRetryCount() >= task.getMaxRetries()) {
				task.setStatus(CompensationTask.STATUS_FAILED);
				compensationTaskRepository.update(consumerName, task);
				log.error("补偿任务超过最大重试次数: consumer={}, key={}", consumerName, task.getIdempotentKey());
				continue;
			}

			if (task.getNextRetryTime() > System.currentTimeMillis()) {
				continue;
			}

			try {
				T event = objectMapper.readValue(task.getEventPayload(), eventClass);
				handler.accept(event);
				compensationTaskRepository.remove(consumerName, task.getIdempotentKey());
				log.info("补偿任务重放成功: consumer={}, key={}", consumerName, task.getIdempotentKey());
			} catch (Exception e) {
				task.setRetryCount(task.getRetryCount() + 1);
				task.setErrorMessage(e.getMessage());
				task.setNextRetryTime(System.currentTimeMillis() + (long) Math.pow(2, task.getRetryCount()) * 1000);
				compensationTaskRepository.update(consumerName, task);
				log.warn("补偿任务重放失败: consumer={}, key={}, retry={}", consumerName, task.getIdempotentKey(), task.getRetryCount());
			}
		}
	}

	public void cleanupCompleted(String consumerName) {
		compensationTaskRepository.cleanupCompleted(consumerName);
	}
}

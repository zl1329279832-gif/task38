package cn.sticki.common.amqp.compensation;

import cn.sticki.common.amqp.event.BaseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

/**
 * 补偿任务服务，负责在消费者降级时保存失败事件，恢复后重放
 * <p>
 * 重放时借助 EventIdempotencyService 保证不会重复计分
 */
@Slf4j
@Service
public class CompensationTaskService {

	private static final int DEFAULT_MAX_RETRIES = 3;

	@Resource
	private CompensationTaskRepository compensationTaskRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * 保存失败事件为补偿任务
	 *
	 * @param event        失败的事件
	 * @param consumerName 消费者名称（用于分组）
	 * @param error        异常信息
	 */
	public void saveForRetry(BaseEvent event, String consumerName, Exception error) {
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
			task.setNextRetryTime(System.currentTimeMillis() + 60_000); // 1分钟后重试
			task.setErrorMessage(error.getMessage() != null ? error.getMessage() : error.getClass().getName());

			compensationTaskRepository.save(consumerName, task);
			log.info("补偿任务已保存: consumer={}, eventType={}, key={}",
					consumerName, event.getEventType(), event.getIdempotentKey());
		} catch (Exception e) {
			log.error("保存补偿任务失败: consumer={}, error={}", consumerName, e.getMessage());
		}
	}

	/**
	 * 重放待处理的补偿任务
	 * <p>
	 * 由于 EventIdempotencyService 的幂等保护，即使事件已被成功处理，
	 * 重放时也不会重复计分（tryConsume 会返回 false）
	 *
	 * @param consumerName 消费者名称
	 * @param handler      事件处理器
	 * @param eventClass   事件类型
	 */
	public <T extends BaseEvent> void replayPendingTasks(String consumerName, Consumer<T> handler, Class<T> eventClass) {
		List<CompensationTask> tasks = compensationTaskRepository.findPendingTasks(consumerName);
		if (tasks.isEmpty()) {
			return;
		}
		log.info("开始重放补偿任务: consumer={}, count={}", consumerName, tasks.size());

		for (CompensationTask task : tasks) {
			if (task.getRetryCount() >= task.getMaxRetries()) {
				task.setStatus(CompensationTask.STATUS_FAILED);
				compensationTaskRepository.update(consumerName, task);
				log.warn("补偿任务超过最大重试次数: consumer={}, key={}", consumerName, task.getIdempotentKey());
				continue;
			}
			if (System.currentTimeMillis() < task.getNextRetryTime()) {
				continue; // 未到重试时间
			}
			try {
				T event = objectMapper.readValue(task.getEventPayload(), eventClass);
				handler.accept(event);
				// 处理成功，删除任务
				compensationTaskRepository.remove(consumerName, task.getIdempotentKey());
				log.info("补偿任务重放成功: consumer={}, key={}", consumerName, task.getIdempotentKey());
			} catch (Exception e) {
				task.setRetryCount(task.getRetryCount() + 1);
				task.setNextRetryTime(System.currentTimeMillis() + 60_000L * task.getRetryCount());
				task.setErrorMessage(e.getMessage());
				compensationTaskRepository.update(consumerName, task);
				log.warn("补偿任务重放失败: consumer={}, key={}, retry={}/{}",
						consumerName, task.getIdempotentKey(), task.getRetryCount(), task.getMaxRetries());
			}
		}
	}

	/**
	 * 清理已完成和已失败的任务
	 */
	public void cleanupCompleted(String consumerName) {
		compensationTaskRepository.cleanupCompleted(consumerName);
	}
}

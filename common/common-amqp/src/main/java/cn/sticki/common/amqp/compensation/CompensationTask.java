package cn.sticki.common.amqp.compensation;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 补偿任务实体，记录消费失败的事件以便后续重放
 */
@Data
@NoArgsConstructor
public class CompensationTask implements Serializable {

	/** 事件类型，如 "blog.like", "user.follow" */
	private String eventType;

	/** 事件JSON序列化内容 */
	private String eventPayload;

	/** 事件的Java类全限定名，反序列化时使用 */
	private String eventClassName;

	/** 幂等键，与BaseEvent.idempotentKey一致 */
	private String idempotentKey;

	/** 任务状态: PENDING / COMPLETED / FAILED */
	private String status;

	/** 已重试次数 */
	private int retryCount;

	/** 最大重试次数 */
	private int maxRetries;

	/** 创建时间戳 (epoch millis) */
	private long createTime;

	/** 下次重试时间戳 (epoch millis) */
	private long nextRetryTime;

	/** 错误信息 */
	private String errorMessage;

	public static final String STATUS_PENDING = "PENDING";
	public static final String STATUS_COMPLETED = "COMPLETED";
	public static final String STATUS_FAILED = "FAILED";
}

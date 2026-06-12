package cn.sticki.common.amqp.event;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

/**
 * MQ事件基类，提供幂等性和排序保证
 */
@Data
@NoArgsConstructor
public abstract class BaseEvent implements Serializable {
	/** 全局唯一事件ID (UUID) */
	private String eventId;
	/** 事件时间戳 (epoch millis)，用于版本比较拒绝过期事件 */
	private long timestamp;
	/** 事件类型标识，如 "blog.insert", "user.follow" */
	private String eventType;
	/** 基于业务语义的确定性幂等键，同一操作始终产生相同key */
	private String idempotentKey;
	/** 事件模型版本号，用于 schema 演进，默认 1 */
	private int eventVersion = 1;
}

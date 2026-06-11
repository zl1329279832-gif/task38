package cn.sticki.common.amqp.event;

import lombok.Data;

import java.io.Serializable;

/**
 * 事件基类，所有跨服务事件消息的父类。
 * 提供幂等键和时间戳，用于事件去重与乱序保护。
 */
@Data
public class BaseEvent implements Serializable {

	/**
	 * 事件唯一ID
	 */
	private String eventId;

	/**
	 * 事件产生时间戳（毫秒）
	 */
	private long timestamp;

	/**
	 * 事件类型标识
	 */
	private String eventType;

	/**
	 * 幂等键，同一业务操作使用相同的幂等键
	 */
	private String idempotentKey;

}

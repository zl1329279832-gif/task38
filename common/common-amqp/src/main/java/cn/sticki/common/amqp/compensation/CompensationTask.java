package cn.sticki.common.amqp.compensation;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class CompensationTask implements Serializable {

	public static final String STATUS_PENDING = "PENDING";
	public static final String STATUS_COMPLETED = "COMPLETED";
	public static final String STATUS_FAILED = "FAILED";

	private String eventType;
	private String eventPayload;
	private String eventClassName;
	private String idempotentKey;
	private String status;
	private int retryCount;
	private int maxRetries;
	private long createTime;
	private long nextRetryTime;
	private String errorMessage;

}

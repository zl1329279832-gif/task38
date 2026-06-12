package cn.sticki.event.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("event_ledger")
public class EventLedgerEntry {

	@TableId(type = IdType.AUTO)
	private Long id;
	private String eventId;
	private String eventType;
	private Integer eventVersion;
	private String idempotentKey;
	private String eventPayload;
	private String sourceService;
	private String relatedUserIds;
	private Long timestamp;
	private LocalDateTime createdAt;
}

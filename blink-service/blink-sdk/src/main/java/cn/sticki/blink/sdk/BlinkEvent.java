package cn.sticki.blink.sdk;

import cn.sticki.common.amqp.event.BaseEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BlinkEvent extends BaseEvent {
	private Integer blinkId;
	private Integer userId;

	public static BlinkEvent ofInsert(Integer blinkId, Integer userId) {
		BlinkEvent event = new BlinkEvent();
		event.setEventId(UUID.randomUUID().toString());
		event.setTimestamp(System.currentTimeMillis());
		event.setEventType(BlinkMqConstants.BLINK_INSERT_KEY);
		event.setIdempotentKey("blink:insert:" + blinkId);
		event.setBlinkId(blinkId);
		event.setUserId(userId);
		return event;
	}

	public static BlinkEvent ofDelete(Integer blinkId, Integer userId) {
		BlinkEvent event = new BlinkEvent();
		event.setEventId(UUID.randomUUID().toString());
		event.setTimestamp(System.currentTimeMillis());
		event.setEventType(BlinkMqConstants.BLINK_DELETE_KEY);
		event.setIdempotentKey("blink:delete:" + blinkId);
		event.setBlinkId(blinkId);
		event.setUserId(userId);
		return event;
	}
}

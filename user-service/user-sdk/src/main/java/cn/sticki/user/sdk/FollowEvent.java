package cn.sticki.user.sdk;

import cn.sticki.common.amqp.event.BaseEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FollowEvent extends BaseEvent {
	private Integer fansId;
	private Integer followId;
	private boolean followed;

	public static FollowEvent of(Integer fansId, Integer followId, boolean followed) {
		FollowEvent event = new FollowEvent();
		event.setEventId(UUID.randomUUID().toString());
		event.setTimestamp(System.currentTimeMillis());
		event.setEventType(followed ? UserMqConstants.USER_FOLLOW_KEY : UserMqConstants.USER_UNFOLLOW_KEY);
		event.setIdempotentKey("user:follow:" + fansId + ":" + followId);
		event.setFansId(fansId);
		event.setFollowId(followId);
		event.setFollowed(followed);
		return event;
	}
}

package cn.sticki.comment.sdk;

import cn.sticki.common.amqp.event.BaseEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
 * 评论操作事件，继承 BaseEvent 以支持幂等消费。
 * 提供工厂方法创建评论增减事件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CommentEvent extends BaseEvent {

	private Integer blogId;

	private Integer userId;

	private String content;

	private Integer authorId;

	private Integer commentId;

	public static CommentEvent ofIncrease(Integer blogId, Integer userId, String content, Integer authorId, Integer commentId) {
		return create(blogId, userId, content, authorId, commentId, "comment.increase");
	}

	public static CommentEvent ofDecrease(Integer blogId, Integer userId, Integer authorId, Integer commentId) {
		return create(blogId, userId, null, authorId, commentId, "comment.decrease");
	}

	private static CommentEvent create(Integer blogId, Integer userId, String content, Integer authorId, Integer commentId, String eventType) {
		CommentEvent event = new CommentEvent();
		event.setBlogId(blogId);
		event.setUserId(userId);
		event.setContent(content);
		event.setAuthorId(authorId);
		event.setCommentId(commentId);
		event.setEventId(UUID.randomUUID().toString());
		event.setTimestamp(System.currentTimeMillis());
		event.setEventType(eventType);
		event.setIdempotentKey(eventType + ":" + blogId + ":" + commentId);
		return event;
	}

}

package cn.sticki.comment.sdk;

import cn.sticki.common.amqp.event.BaseEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 评论操作事件
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CommentEvent extends BaseEvent {

	private Integer blogId;
	private Integer userId;
	private String content;
	private Integer authorId;
	private Integer commentId;

	private static CommentEvent create(Integer blogId, Integer userId, String content, Integer authorId, Integer commentId, String eventType, String idempotentKey) {
		CommentEvent event = new CommentEvent();
		event.setEventId(UUID.randomUUID().toString());
		event.setTimestamp(System.currentTimeMillis());
		event.setEventType(eventType);
		event.setIdempotentKey(idempotentKey);
		event.setBlogId(blogId);
		event.setUserId(userId);
		event.setContent(content);
		event.setAuthorId(authorId);
		event.setCommentId(commentId);
		return event;
	}

	public static CommentEvent ofIncrease(Integer blogId, Integer userId, String content, Integer authorId, Integer commentId) {
		return create(blogId, userId, content, authorId, commentId, MqConstants.BLOG_COMMENT_INCREASE_KEY, "comment:increase:" + commentId);
	}

	public static CommentEvent ofDecrease(Integer blogId, Integer userId, Integer authorId, Integer commentId) {
		return create(blogId, userId, null, authorId, commentId, MqConstants.BLOG_COMMENT_DECREASE_KEY, "comment:decrease:" + commentId);
	}
}

package cn.sticki.blog.sdk;

import cn.sticki.common.amqp.event.BaseEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 博客操作事件
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BlogEvent extends BaseEvent {

	private Integer blogId;
	private Integer userId;
	private Integer authorId;

	private static BlogEvent create(Integer blogId, Integer userId, Integer authorId, String eventType, String idempotentKey) {
		BlogEvent event = new BlogEvent();
		event.setEventId(UUID.randomUUID().toString());
		event.setTimestamp(System.currentTimeMillis());
		event.setEventType(eventType);
		event.setIdempotentKey(idempotentKey);
		event.setBlogId(blogId);
		event.setUserId(userId);
		event.setAuthorId(authorId);
		return event;
	}

	public static BlogEvent ofInsert(Integer blogId, Integer authorId) {
		return create(blogId, null, authorId, BlogMqConstants.BLOG_INSERT_KEY, "blog:insert:" + blogId);
	}

	public static BlogEvent ofUpdate(Integer blogId, Integer authorId) {
		long ts = System.currentTimeMillis();
		return create(blogId, null, authorId, BlogMqConstants.BLOG_UPDATE_KEY, "blog:update:" + blogId + ":" + ts);
	}

	public static BlogEvent ofDelete(Integer blogId, Integer authorId) {
		return create(blogId, null, authorId, BlogMqConstants.BLOG_DELETE_KEY, "blog:delete:" + blogId);
	}

	public static BlogEvent ofRead(Integer blogId, Integer userId, Integer authorId) {
		return create(blogId, userId, authorId, BlogMqConstants.BLOG_OPERATE_READ_KEY, "blog:read:" + userId + ":" + blogId);
	}

	public static BlogEvent ofLike(Integer blogId, Integer userId, Integer authorId) {
		return create(blogId, userId, authorId, BlogMqConstants.BLOG_OPERATE_LIKE_KEY, "blog:like:" + userId + ":" + blogId);
	}

	public static BlogEvent ofLikeCancel(Integer blogId, Integer userId, Integer authorId) {
		return create(blogId, userId, authorId, BlogMqConstants.BLOG_OPERATE_LIKE_CANCEL_KEY, "blog:like:cancel:" + userId + ":" + blogId);
	}

	public static BlogEvent ofCollect(Integer blogId, Integer userId, Integer authorId) {
		return create(blogId, userId, authorId, BlogMqConstants.BLOG_OPERATE_COLLECT_KEY, "blog:collect:" + userId + ":" + blogId);
	}

	public static BlogEvent ofCollectCancel(Integer blogId, Integer userId, Integer authorId) {
		return create(blogId, userId, authorId, BlogMqConstants.BLOG_OPERATE_COLLECT_CANCEL_KEY, "blog:collect:cancel:" + userId + ":" + blogId);
	}

	public static BlogEvent ofRelay(Integer blogId, Integer userId, Integer authorId) {
		return create(blogId, userId, authorId, BlogMqConstants.BLOG_OPERATE_RELAY_KEY, "blog:relay:" + userId + ":" + blogId);
	}
}

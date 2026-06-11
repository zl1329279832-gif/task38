package cn.sticki.blog.sdk;

import cn.sticki.common.amqp.event.BaseEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
 * 博客操作事件，继承 BaseEvent 以支持幂等消费。
 * 提供工厂方法创建不同类型的博客事件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BlogEvent extends BaseEvent {

	private Integer blogId;

	private Integer userId;

	private Integer authorId;

	public static BlogEvent ofRead(Integer blogId, Integer userId, Integer authorId) {
		return create(blogId, userId, authorId, "blog.read");
	}

	public static BlogEvent ofLike(Integer blogId, Integer userId, Integer authorId) {
		return create(blogId, userId, authorId, "blog.like");
	}

	public static BlogEvent ofCollect(Integer blogId, Integer userId, Integer authorId) {
		return create(blogId, userId, authorId, "blog.collect");
	}

	public static BlogEvent ofInsert(Integer blogId, Integer userId, Integer authorId) {
		return create(blogId, userId, authorId, "blog.insert");
	}

	public static BlogEvent ofDelete(Integer blogId, Integer userId, Integer authorId) {
		return create(blogId, userId, authorId, "blog.delete");
	}

	public static BlogEvent ofLikeCancel(Integer blogId, Integer userId, Integer authorId) {
		return create(blogId, userId, authorId, "blog.like.cancel");
	}

	public static BlogEvent ofCollectCancel(Integer blogId, Integer userId, Integer authorId) {
		return create(blogId, userId, authorId, "blog.collect.cancel");
	}

	private static BlogEvent create(Integer blogId, Integer userId, Integer authorId, String eventType) {
		BlogEvent event = new BlogEvent();
		event.setBlogId(blogId);
		event.setUserId(userId);
		event.setAuthorId(authorId);
		event.setEventId(UUID.randomUUID().toString());
		event.setTimestamp(System.currentTimeMillis());
		event.setEventType(eventType);
		event.setIdempotentKey(eventType + ":" + blogId + ":" + userId);
		return event;
	}

}

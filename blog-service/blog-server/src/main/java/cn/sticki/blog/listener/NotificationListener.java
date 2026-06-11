package cn.sticki.blog.listener;

import cn.sticki.blog.sdk.BlogEvent;
import cn.sticki.blog.sdk.BlogMqConstants;
import cn.sticki.blog.service.NotificationService;
import cn.sticki.comment.sdk.CommentEvent;
import cn.sticki.comment.sdk.MqConstants;
import cn.sticki.common.amqp.autoconfig.EventIdempotencyService;
import cn.sticki.user.sdk.FollowEvent;
import cn.sticki.user.sdk.UserMqConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationListener {

	private static final String NOTIFY_LIKE_QUEUE = "notify.blog.like";
	private static final String NOTIFY_COLLECT_QUEUE = "notify.blog.collect";
	private static final String NOTIFY_COMMENT_QUEUE = "notify.blog.comment";
	private static final String NOTIFY_FOLLOW_QUEUE = "notify.user.follow";

	@Resource
	private NotificationService notificationService;

	@Resource
	private EventIdempotencyService eventIdempotencyService;

	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = BlogMqConstants.BLOG_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = NOTIFY_LIKE_QUEUE),
			key = BlogMqConstants.BLOG_OPERATE_LIKE_KEY
	))
	public void onBlogLike(BlogEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		// Skip self-action
		if (event.getUserId() != null && event.getUserId().equals(event.getAuthorId())) return;
		log.debug("点赞通知: userId={}, blogId={}", event.getAuthorId(), event.getBlogId());
		notificationService.createNotification(
				event.getAuthorId(), 2, "有人赞了你的博客",
				null, event.getBlogId(), "blog", event.getUserId());
	}

	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = BlogMqConstants.BLOG_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = NOTIFY_COLLECT_QUEUE),
			key = BlogMqConstants.BLOG_OPERATE_COLLECT_KEY
	))
	public void onBlogCollect(BlogEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		if (event.getUserId() != null && event.getUserId().equals(event.getAuthorId())) return;
		log.debug("收藏通知: userId={}, blogId={}", event.getAuthorId(), event.getBlogId());
		notificationService.createNotification(
				event.getAuthorId(), 3, "有人收藏了你的博客",
				null, event.getBlogId(), "blog", event.getUserId());
	}

	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = MqConstants.COMMENT_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = NOTIFY_COMMENT_QUEUE),
			key = MqConstants.BLOG_COMMENT_INCREASE_KEY
	))
	public void onBlogComment(CommentEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		if (event.getUserId() != null && event.getUserId().equals(event.getAuthorId())) return;
		log.debug("评论通知: userId={}, blogId={}", event.getAuthorId(), event.getBlogId());
		notificationService.createNotification(
				event.getAuthorId(), 4, "有人评论了你的博客",
				event.getContent(), event.getBlogId(), "blog", event.getUserId());
	}

	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = UserMqConstants.USER_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = NOTIFY_FOLLOW_QUEUE),
			key = UserMqConstants.USER_FOLLOW_KEY
	))
	public void onUserFollow(FollowEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		log.debug("关注通知: userId={}, fansId={}", event.getFollowId(), event.getFansId());
		notificationService.createNotification(
				event.getFollowId(), 1, "有人关注了你",
				null, event.getFansId(), "user", event.getFansId());
	}

}

package cn.sticki.blog.listener;

import cn.sticki.blog.sdk.BlogEvent;
import cn.sticki.blog.sdk.BlogMqConstants;
import cn.sticki.blog.service.CompensationService;
import cn.sticki.blog.service.FeedService;
import cn.sticki.blink.sdk.BlinkEvent;
import cn.sticki.blink.sdk.BlinkMqConstants;
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
public class FeedListener {

	private static final String FEED_PUBLISH_QUEUE = "blog.feed.publish";

	private static final String FEED_FOLLOW_QUEUE = "blog.feed.follow";

	private static final String FEED_UNFOLLOW_QUEUE = "blog.feed.unfollow";

	private static final String FEED_BLINK_QUEUE = "blog.feed.blink";

	@Resource
	private FeedService feedService;

	@Resource
	private EventIdempotencyService eventIdempotencyService;

	@Resource
	private CompensationService compensationService;

	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = BlogMqConstants.BLOG_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = FEED_PUBLISH_QUEUE),
			key = BlogMqConstants.BLOG_INSERT_KEY
	))
	public void onBlogPublish(BlogEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		log.debug("博客发布推送到关注流: blogId={}", event.getBlogId());
		try {
			feedService.pushBlogToFollowers(event.getAuthorId(), event.getBlogId(), event.getTimestamp() / 1000);
		} catch (Exception e) {
			log.warn("关注流推送失败，写入补偿任务: blogId={}, error={}", event.getBlogId(), e.getMessage());
			compensationService.saveCompensation("feed:push",
					event.getAuthorId() + ":" + event.getBlogId() + ":" + (event.getTimestamp() / 1000),
					event.getTimestamp());
		}
	}

	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = UserMqConstants.USER_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = FEED_FOLLOW_QUEUE),
			key = UserMqConstants.USER_FOLLOW_KEY
	))
	public void onFollow(FollowEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		log.debug("关注回填: fansId={}, followId={}", event.getFansId(), event.getFollowId());
		try {
			feedService.backfillOnFollow(event.getFansId(), event.getFollowId());
		} catch (Exception e) {
			log.warn("关注回填失败，写入补偿任务: fansId={}, followId={}, error={}",
					event.getFansId(), event.getFollowId(), e.getMessage());
			compensationService.saveCompensation("feed:backfill",
					event.getFansId() + ":" + event.getFollowId(),
					event.getTimestamp());
		}
	}

	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = UserMqConstants.USER_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = FEED_UNFOLLOW_QUEUE),
			key = UserMqConstants.USER_UNFOLLOW_KEY
	))
	public void onUnfollow(FollowEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		log.debug("取关清理: fansId={}, followId={}", event.getFansId(), event.getFollowId());
		try {
			feedService.cleanupOnUnfollow(event.getFansId(), event.getFollowId());
		} catch (Exception e) {
			log.warn("取关清理失败，写入补偿任务: fansId={}, followId={}, error={}",
					event.getFansId(), event.getFollowId(), e.getMessage());
			compensationService.saveCompensation("feed:cleanup",
					event.getFansId() + ":" + event.getFollowId(),
					event.getTimestamp());
		}
	}

	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = BlinkMqConstants.BLINK_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = FEED_BLINK_QUEUE),
			key = BlinkMqConstants.BLINK_INSERT_KEY
	))
	public void onBlinkPublish(BlinkEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		log.debug("动态发布推送到关注流: blinkId={}", event.getBlinkId());
		try {
			feedService.pushBlinkToFollowers(event.getUserId(), event.getBlinkId(), event.getTimestamp() / 1000);
		} catch (Exception e) {
			log.warn("动态推送失败，写入补偿任务: blinkId={}, error={}", event.getBlinkId(), e.getMessage());
			compensationService.saveCompensation("feed:blink",
					event.getUserId() + ":" + event.getBlinkId() + ":" + (event.getTimestamp() / 1000),
					event.getTimestamp());
		}
	}

}

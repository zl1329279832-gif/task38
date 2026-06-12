package cn.sticki.event.listener;

import cn.sticki.blink.sdk.BlinkEvent;
import cn.sticki.blink.sdk.BlinkMqConstants;
import cn.sticki.blog.sdk.BlogEvent;
import cn.sticki.blog.sdk.BlogMqConstants;
import cn.sticki.comment.sdk.CommentEvent;
import cn.sticki.comment.sdk.MqConstants;
import cn.sticki.common.amqp.event.BaseEvent;
import cn.sticki.event.service.EventLedgerService;
import cn.sticki.resource.sdk.ResourceEvent;
import cn.sticki.resource.sdk.ResourceMqConstants;
import cn.sticki.user.sdk.FollowEvent;
import cn.sticki.user.sdk.UserMqConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
public class EventLedgerListener {

	@Resource
	private EventLedgerService eventLedgerService;

	// ===== 博客事件 =====

	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = BlogMqConstants.BLOG_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = "event.ledger.blog"),
			key = "blog.#"
	))
	public void onBlogEvent(BlogEvent event) {
		Set<Integer> userIds = EventLedgerService.toUserIdSet(
				event.getAuthorId(), event.getUserId()
		);
		eventLedgerService.writeLedger(event, "blog", userIds);
	}

	// ===== 用户关注事件 =====

	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = UserMqConstants.USER_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = "event.ledger.user"),
			key = "user.#"
	))
	public void onFollowEvent(FollowEvent event) {
		Set<Integer> userIds = EventLedgerService.toUserIdSet(
				event.getFansId(), event.getFollowId()
		);
		eventLedgerService.writeLedger(event, "user", userIds);
	}

	// ===== 评论事件 =====

	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = MqConstants.COMMENT_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = "event.ledger.comment"),
			key = "blog.#"
	))
	public void onCommentEvent(CommentEvent event) {
		Set<Integer> userIds = EventLedgerService.toUserIdSet(
				event.getUserId(), event.getAuthorId()
		);
		eventLedgerService.writeLedger(event, "comment", userIds);
	}

	// ===== 动态事件 =====

	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = BlinkMqConstants.BLINK_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = "event.ledger.blink"),
			key = "blink.#"
	))
	public void onBlinkEvent(BlinkEvent event) {
		Set<Integer> userIds = EventLedgerService.toUserIdSet(event.getUserId());
		eventLedgerService.writeLedger(event, "blink", userIds);
	}

	// ===== 资源事件 =====

	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = ResourceMqConstants.RESOURCE_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = "event.ledger.resource"),
			key = "resource.#"
	))
	public void onResourceEvent(ResourceEvent event) {
		Set<Integer> userIds = EventLedgerService.toUserIdSet(event.getUserId());
		eventLedgerService.writeLedger(event, "resource", userIds);
	}
}

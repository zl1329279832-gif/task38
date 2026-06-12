package cn.sticki.event.listener;

import cn.sticki.blink.sdk.BlinkEvent;
import cn.sticki.blog.sdk.BlogEvent;
import cn.sticki.comment.sdk.CommentEvent;
import cn.sticki.event.service.EventLedgerService;
import cn.sticki.resource.sdk.ResourceEvent;
import cn.sticki.user.sdk.FollowEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static cn.sticki.event.sdk.EventMqConstants.*;

@Slf4j
@Component
public class EventLedgerListener {

	private final EventLedgerService eventLedgerService;

	public EventLedgerListener(EventLedgerService eventLedgerService) {
		this.eventLedgerService = eventLedgerService;
	}

	@RabbitListener(queues = EVENT_LEDGER_BLOG_QUEUE)
	public void onBlogEvent(BlogEvent event) {
		eventLedgerService.writeLedger(event, "blog",
				EventLedgerService.toUserIdSet(event.getAuthorId(), event.getUserId()));
	}

	@RabbitListener(queues = EVENT_LEDGER_USER_QUEUE)
	public void onFollowEvent(FollowEvent event) {
		eventLedgerService.writeLedger(event, "user",
				EventLedgerService.toUserIdSet(event.getFansId(), event.getFollowId()));
	}

	@RabbitListener(queues = EVENT_LEDGER_COMMENT_QUEUE)
	public void onCommentEvent(CommentEvent event) {
		eventLedgerService.writeLedger(event, "comment",
				EventLedgerService.toUserIdSet(event.getUserId(), event.getAuthorId()));
	}

	@RabbitListener(queues = EVENT_LEDGER_BLINK_QUEUE)
	public void onBlinkEvent(BlinkEvent event) {
		eventLedgerService.writeLedger(event, "blink",
				EventLedgerService.toUserIdSet(event.getUserId()));
	}

	@RabbitListener(queues = EVENT_LEDGER_RESOURCE_QUEUE)
	public void onResourceEvent(ResourceEvent event) {
		eventLedgerService.writeLedger(event, "resource",
				EventLedgerService.toUserIdSet(event.getUserId()));
	}

}

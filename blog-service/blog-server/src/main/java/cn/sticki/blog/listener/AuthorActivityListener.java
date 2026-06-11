package cn.sticki.blog.listener;

import cn.sticki.blog.sdk.BlogEvent;
import cn.sticki.blog.sdk.BlogMqConstants;
import cn.sticki.blog.service.RankService;
import cn.sticki.common.amqp.autoconfig.EventIdempotencyService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthorActivityListener {

	private static final String AUTHOR_ACTIVITY_QUEUE = "blog.author.activity";

	@Resource
	private RankService rankService;

	@Resource
	private EventIdempotencyService eventIdempotencyService;

	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = BlogMqConstants.BLOG_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = AUTHOR_ACTIVITY_QUEUE),
			key = "blog.#"
	))
	public void onBlogEvent(BlogEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		Integer authorId = event.getAuthorId();
		if (authorId == null) return;

		String eventType = event.getEventType();
		double delta = switch (eventType) {
			case BlogMqConstants.BLOG_INSERT_KEY -> 5.0;
			case BlogMqConstants.BLOG_OPERATE_READ_KEY -> 1.0;
			case BlogMqConstants.BLOG_OPERATE_LIKE_KEY, BlogMqConstants.BLOG_OPERATE_COLLECT_KEY -> 3.0;
			case BlogMqConstants.BLOG_OPERATE_LIKE_CANCEL_KEY, BlogMqConstants.BLOG_OPERATE_COLLECT_CANCEL_KEY -> -3.0;
			default -> 0.0;
		};

		if (delta != 0) {
			rankService.updateAuthorActivityScore(authorId, delta);
		}
	}

}

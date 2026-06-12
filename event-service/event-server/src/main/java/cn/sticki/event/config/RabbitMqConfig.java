package cn.sticki.event.config;

import cn.sticki.blink.sdk.BlinkMqConstants;
import cn.sticki.blog.sdk.BlogMqConstants;
import cn.sticki.comment.sdk.MqConstants;
import cn.sticki.resource.sdk.ResourceMqConstants;
import cn.sticki.user.sdk.UserMqConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static cn.sticki.event.sdk.EventMqConstants.*;

@Configuration
public class RabbitMqConfig {

	// ===== Queues =====

	@Bean
	public Queue eventLedgerBlogQueue() {
		return new Queue(EVENT_LEDGER_BLOG_QUEUE, true);
	}

	@Bean
	public Queue eventLedgerUserQueue() {
		return new Queue(EVENT_LEDGER_USER_QUEUE, true);
	}

	@Bean
	public Queue eventLedgerCommentQueue() {
		return new Queue(EVENT_LEDGER_COMMENT_QUEUE, true);
	}

	@Bean
	public Queue eventLedgerBlinkQueue() {
		return new Queue(EVENT_LEDGER_BLINK_QUEUE, true);
	}

	@Bean
	public Queue eventLedgerResourceQueue() {
		return new Queue(EVENT_LEDGER_RESOURCE_QUEUE, true);
	}

	// ===== Exchanges =====

	@Bean
	public TopicExchange blogTopicExchange() {
		return new TopicExchange(BlogMqConstants.BLOG_TOPIC_EXCHANGE);
	}

	@Bean
	public TopicExchange userTopicExchange() {
		return new TopicExchange(UserMqConstants.USER_TOPIC_EXCHANGE);
	}

	@Bean
	public TopicExchange commentTopicExchange() {
		return new TopicExchange(MqConstants.COMMENT_TOPIC_EXCHANGE);
	}

	@Bean
	public TopicExchange blinkTopicExchange() {
		return new TopicExchange(BlinkMqConstants.BLINK_TOPIC_EXCHANGE);
	}

	@Bean
	public TopicExchange resourceTopicExchange() {
		return new TopicExchange(ResourceMqConstants.RESOURCE_TOPIC_EXCHANGE);
	}

	// ===== Bindings =====

	@Bean
	public Binding blogLedgerBinding() {
		return BindingBuilder.bind(eventLedgerBlogQueue())
				.to(blogTopicExchange())
				.with("blog.#");
	}

	@Bean
	public Binding userLedgerBinding() {
		return BindingBuilder.bind(eventLedgerUserQueue())
				.to(userTopicExchange())
				.with("user.#");
	}

	@Bean
	public Binding commentLedgerBinding() {
		return BindingBuilder.bind(eventLedgerCommentQueue())
				.to(commentTopicExchange())
				.with("blog.#");
	}

	@Bean
	public Binding blinkLedgerBinding() {
		return BindingBuilder.bind(eventLedgerBlinkQueue())
				.to(blinkTopicExchange())
				.with("blink.#");
	}

	@Bean
	public Binding resourceLedgerBinding() {
		return BindingBuilder.bind(eventLedgerResourceQueue())
				.to(resourceTopicExchange())
				.with("resource.#");
	}

}

package cn.sticki.user.listener;

import cn.sticki.blog.sdk.BlogEvent;
import cn.sticki.common.amqp.autoconfig.EventIdempotencyService;
import cn.sticki.common.amqp.compensation.CompensationTaskService;
import cn.sticki.user.mapper.UserGeneralMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static cn.sticki.blog.sdk.BlogMqConstants.*;

/**
 * @author durance
 * @version 1.0
 * @date 2022/10/5 14:20
 */
@Slf4j
@Component
public class BlogListener {

	public static final String USER_SEE_QUEUE = "user.operate.see";

	public static final String USER_COLLECT_QUEUE = "user.operate.collect";

	public static final String USER_COLLECT_QUEUE_CANCEL = "user.operate.collect.cancel";

	public static final String USER_LIKE_QUEUE = "user.operate.like";

	public static final String USER_LIKE_QUEUE_CANCEL = "user.operate.like.cancel";

	public static final String USER_PUBLISH_QUEUE = "user.operate.publish";

	public static final String USER_PUBLISH_QUEUE_CANCEL = "user.operate.publish.cancel";

	@Resource
	private UserGeneralMapper userGeneralMapper;

	@Resource
	private EventIdempotencyService eventIdempotencyService;

	@Resource
	private CompensationTaskService compensationTaskService;

	/**
	 * 用户访问博客
	 *
	 * @param event 博客事件
	 */
	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = BLOG_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = USER_SEE_QUEUE),
			key = BLOG_OPERATE_READ_KEY
	))
	public void seeAddUserGeneral(BlogEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		try {
			log.debug("用户 {} 访问加1", event.getAuthorId());
			userGeneralMapper.updateViewNumByUserId(event.getAuthorId());
		} catch (Exception e) {
			compensationTaskService.saveForRetry(event, "user.operate.see", e);
			throw e;
		}
	}

	/**
	 * 用户收藏博客
	 *
	 * @param event 博客事件
	 */
	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = BLOG_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = USER_COLLECT_QUEUE),
			key = BLOG_OPERATE_COLLECT_KEY
	))
	public void collectAddUserGeneral(BlogEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		try {
			log.debug("用户 {} 收藏加1", event.getBlogId());
			userGeneralMapper.updateCollectNumByUserId(event.getAuthorId(), 1);
		} catch (Exception e) {
			compensationTaskService.saveForRetry(event, "user.operate.collect", e);
			throw e;
		}
	}

	/**
	 * 用户取消收藏博客
	 *
	 * @param event 博客事件
	 */
	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = BLOG_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = USER_COLLECT_QUEUE_CANCEL),
			key = BLOG_OPERATE_COLLECT_CANCEL_KEY
	))
	public void collectReduceUserGeneral(BlogEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		try {
			log.debug("用户 {} 收藏加-1", event.getBlogId());
			userGeneralMapper.updateCollectNumByUserId(event.getAuthorId(), -1);
		} catch (Exception e) {
			compensationTaskService.saveForRetry(event, "user.operate.collect.cancel", e);
			throw e;
		}
	}

	/**
	 * 用户点赞博客
	 *
	 * @param event 博客事件
	 */
	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = BLOG_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = USER_LIKE_QUEUE),
			key = BLOG_OPERATE_LIKE_KEY
	))
	public void likeAddUserGeneral(BlogEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		try {
			log.debug("用户 {} 点赞加1", event.getBlogId());
			userGeneralMapper.updateLikeNumByUserId(event.getAuthorId(), 1);
		} catch (Exception e) {
			compensationTaskService.saveForRetry(event, "user.operate.like", e);
			throw e;
		}
	}

	/**
	 * 用户取消点赞博客
	 *
	 * @param event 博客事件
	 */
	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = BLOG_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = USER_LIKE_QUEUE_CANCEL),
			key = BLOG_OPERATE_LIKE_CANCEL_KEY
	))
	public void likeReduceUserGeneral(BlogEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		try {
			log.debug("用户 {} 点赞加-1", event.getBlogId());
			userGeneralMapper.updateLikeNumByUserId(event.getAuthorId(), -1);
		} catch (Exception e) {
			compensationTaskService.saveForRetry(event, "user.operate.like.cancel", e);
			throw e;
		}
	}

	/**
	 * 用户发布博客
	 *
	 * @param event 博客事件
	 */
	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = BLOG_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = USER_PUBLISH_QUEUE),
			key = BLOG_INSERT_KEY
	))
	public void publishAddUserGeneral(BlogEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		try {
			log.debug("用户 {} 发布博客数加1", event.getAuthorId());
			userGeneralMapper.updateBlogNumByUserId(event.getAuthorId(), 1);
		} catch (Exception e) {
			compensationTaskService.saveForRetry(event, "user.operate.publish", e);
			throw e;
		}
	}

	/**
	 * 用户删除博客
	 *
	 * @param event 博客事件
	 */
	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = BLOG_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = USER_PUBLISH_QUEUE_CANCEL),
			key = BLOG_DELETE_KEY
	))
	public void publishReduceUserGeneral(BlogEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		try {
			log.debug("用户 {} 删除博客数 -1", event.getAuthorId());
			userGeneralMapper.updateBlogNumByUserId(event.getAuthorId(), -1);
		} catch (Exception e) {
			compensationTaskService.saveForRetry(event, "user.operate.publish.cancel", e);
			throw e;
		}
	}

}

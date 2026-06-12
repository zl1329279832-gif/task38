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
 * 监听Blog模块的消息，更新用户统计数据
 * <p>
 * 使用 BlogEvent（继承 BaseEvent）替代旧的 BlogOperateDTO，
 * 通过 EventIdempotencyService 保证事件幂等性，防止重复投递导致统计计数错误。
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
	 * 用户访问博客 - 浏览量+1
	 */
	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = BLOG_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = USER_SEE_QUEUE),
			key = BLOG_OPERATE_READ_KEY
	))
	public void seeAddUserGeneral(BlogEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		log.debug("用户 {} 访问加1", event.getAuthorId());
		userGeneralMapper.updateViewNumByUserId(event.getAuthorId());
	}

	/**
	 * 用户收藏博客 - 收藏数+1
	 */
	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = BLOG_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = USER_COLLECT_QUEUE),
			key = BLOG_OPERATE_COLLECT_KEY
	))
	public void collectAddUserGeneral(BlogEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		log.debug("用户 {} 收藏加1", event.getBlogId());
		userGeneralMapper.updateCollectNumByUserId(event.getAuthorId(), 1);
	}

	/**
	 * 用户取消收藏博客 - 收藏数-1
	 */
	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = BLOG_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = USER_COLLECT_QUEUE_CANCEL),
			key = BLOG_OPERATE_COLLECT_CANCEL_KEY
	))
	public void collectReduceUserGeneral(BlogEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		log.debug("用户 {} 收藏加-1", event.getBlogId());
		userGeneralMapper.updateCollectNumByUserId(event.getAuthorId(), -1);
	}

	/**
	 * 用户点赞博客 - 点赞数+1
	 */
	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = BLOG_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = USER_LIKE_QUEUE),
			key = BLOG_OPERATE_LIKE_KEY
	))
	public void likeAddUserGeneral(BlogEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		log.debug("用户 {} 点赞加1", event.getBlogId());
		try {
			userGeneralMapper.updateLikeNumByUserId(event.getAuthorId(), 1);
		} catch (RuntimeException e) {
			compensationTaskService.saveForRetry(event, USER_LIKE_QUEUE, e);
			throw e;
		}
	}

	/**
	 * 用户取消点赞博客 - 点赞数-1
	 */
	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = BLOG_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = USER_LIKE_QUEUE_CANCEL),
			key = BLOG_OPERATE_LIKE_CANCEL_KEY
	))
	public void likeReduceUserGeneral(BlogEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		log.debug("用户 {} 点赞-1", event.getBlogId());
		userGeneralMapper.updateLikeNumByUserId(event.getAuthorId(), -1);
	}

	/**
	 * 用户发布博客 - 博客数+1
	 */
	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = BLOG_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = USER_PUBLISH_QUEUE),
			key = BLOG_INSERT_KEY
	))
	public void publishAddUserGeneral(BlogEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		log.debug("用户 {} 发布博客数加1", event.getAuthorId());
		userGeneralMapper.updateBlogNumByUserId(event.getAuthorId(), 1);
	}

	/**
	 * 用户删除博客 - 博客数-1
	 */
	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = BLOG_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = USER_PUBLISH_QUEUE_CANCEL),
			key = BLOG_DELETE_KEY
	))
	public void publishReduceUserGeneral(BlogEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		log.debug("用户 {} 删除博客数 -1", event.getAuthorId());
		userGeneralMapper.updateBlogNumByUserId(event.getAuthorId(), -1);
	}

}

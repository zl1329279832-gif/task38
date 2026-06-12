package cn.sticki.blog.listener;

import cn.sticki.blog.mapper.BlogGeneralMapper;
import cn.sticki.blog.service.RankService;
import cn.sticki.blog.service.impl.BlogStatsCacheService;
import cn.sticki.comment.sdk.CommentEvent;
import cn.sticki.common.amqp.autoconfig.EventIdempotencyService;
import cn.sticki.common.amqp.compensation.CompensationTaskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

import static cn.sticki.comment.sdk.MqConstants.*;

/**
 * 监听Comment模块的消息
 * <p>
 * todo 取消的动作也是需要减分的，参考BlogListener
 *
 * @author 阿杆
 * @version 1.0
 * @date 2022/6/26 11:52
 */
@Slf4j
@Component
public class CommentListener {

	private static final String COMMENT_INCREASE_QUEUE = "blog.comment.increase";

	private static final String COMMENT_DECREASE_QUEUE = "blog.comment.decrease";

	@Resource
	private BlogGeneralMapper blogGeneralMapper;

	@Resource
	private RankService rankService;

	@Resource
	private EventIdempotencyService eventIdempotencyService;

	@Resource
	private BlogStatsCacheService blogStatsCacheService;

	@Resource
	private CompensationTaskService compensationTaskService;

	/**
	 * 博客评论数量增加
	 *
	 * @param event 博客评论事件
	 */
	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = COMMENT_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = COMMENT_INCREASE_QUEUE),
			key = BLOG_COMMENT_INCREASE_KEY
	))
	public void commentNumberIncreaseListener(CommentEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		log.debug("{} 评论数量+1", event.getBlogId());
		try {
			blogGeneralMapper.increaseCommentNum(event.getBlogId());
			blogStatsCacheService.invalidate(event.getBlogId());
			rankService.recalculateBlogHotScore(event.getBlogId());
		} catch (RuntimeException e) {
			compensationTaskService.saveForRetry(event, COMMENT_INCREASE_QUEUE, e);
			throw e;
		}
	}

	/**
	 * 博客评论数量减少
	 *
	 * @param event 博客评论事件
	 */
	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = COMMENT_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = COMMENT_DECREASE_QUEUE),
			key = BLOG_COMMENT_DECREASE_KEY
	))
	public void commentNumberDecreaseListener(CommentEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		// 减少博客的评论数量
		log.debug("{} 评论数量-1", event.getBlogId());
		blogGeneralMapper.decreaseCommentNum(event.getBlogId());
		// 使博客统计缓存失效，确保重算时使用最新数据
		blogStatsCacheService.invalidate(event.getBlogId());
		// 重新计算博客热榜分数（含时间衰减和风控）
		rankService.recalculateBlogHotScore(event.getBlogId());
	}

}

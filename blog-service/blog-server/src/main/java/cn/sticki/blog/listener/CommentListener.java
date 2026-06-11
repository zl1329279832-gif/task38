package cn.sticki.blog.listener;

import cn.sticki.blog.mapper.BlogGeneralMapper;
import cn.sticki.blog.service.RankService;
import cn.sticki.comment.sdk.CommentEvent;
import cn.sticki.common.amqp.autoconfig.EventIdempotencyService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static cn.sticki.comment.sdk.MqConstants.*;

/**
 * 监听Comment模块的消息，使用幂等事件驱动评论数与热榜更新
 *
 * @author 阿杆
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

	/**
	 * 博客评论数量增加
	 */
	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = COMMENT_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = COMMENT_INCREASE_QUEUE),
			key = BLOG_COMMENT_INCREASE_KEY
	))
	public void commentNumberIncreaseListener(CommentEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) {
			log.debug("重复评论增加事件被拒绝: {}", event.getIdempotentKey());
			return;
		}
		log.debug("{} 评论数量+1", event.getBlogId());
		blogGeneralMapper.increaseCommentNum(event.getBlogId());
		rankService.recalculateBlogHotScore(event.getBlogId());
	}

	/**
	 * 博客评论数量减少（删除评论回滚）
	 */
	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = COMMENT_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = COMMENT_DECREASE_QUEUE),
			key = BLOG_COMMENT_DECREASE_KEY
	))
	public void commentNumberDecreaseListener(CommentEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) {
			log.debug("重复评论减少事件被拒绝: {}", event.getIdempotentKey());
			return;
		}
		log.debug("{} 评论数量-1", event.getBlogId());
		blogGeneralMapper.decreaseCommentNum(event.getBlogId());
		rankService.recalculateBlogHotScore(event.getBlogId());
	}

}

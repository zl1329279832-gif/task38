package cn.sticki.blog.listener;

import cn.sticki.blog.sdk.BlogEvent;
import cn.sticki.blog.service.RankService;
import cn.sticki.common.amqp.autoconfig.EventIdempotencyService;
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
 * 监听Blog模块的消息，使用幂等事件驱动热榜重算与作者活跃度更新
 *
 * @author durance
 */
@Slf4j
@Component
public class BlogListener {

	public static final String SEE_RANK_QUEUE = "blog.rank.see";

	public static final String COLLECT_RANK_QUEUE = "blog.rank.collect";

	public static final String LIKE_RANK_QUEUE = "blog.rank.like";

	@Resource
	private RankService rankService;

	@Resource
	private EventIdempotencyService eventIdempotencyService;

	/**
	 * 用户浏览博客，重算热榜分数并增加作者活跃度
	 */
	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = BLOG_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = SEE_RANK_QUEUE),
			key = BLOG_OPERATE_READ_KEY
	))
	public void seeAddRankHotScore(BlogEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) {
			log.debug("重复事件被拒绝: {}", event.getIdempotentKey());
			return;
		}
		log.debug("{} 被浏览，重算热榜", event.getBlogId());
		rankService.recalculateBlogHotScore(event.getBlogId());
		rankService.updateAuthorActivityScore(event.getAuthorId(), 1.0);
	}

	/**
	 * 用户收藏博客，重算热榜分数并增加作者活跃度
	 */
	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = BLOG_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = COLLECT_RANK_QUEUE),
			key = BLOG_OPERATE_COLLECT_KEY
	))
	public void collectAddRankHotScore(BlogEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) {
			log.debug("重复事件被拒绝: {}", event.getIdempotentKey());
			return;
		}
		log.debug("{} 被收藏，重算热榜", event.getBlogId());
		rankService.recalculateBlogHotScore(event.getBlogId());
		rankService.updateAuthorActivityScore(event.getAuthorId(), 3.0);
	}

	/**
	 * 用户点赞博客，重算热榜分数并增加作者活跃度
	 */
	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = BLOG_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = LIKE_RANK_QUEUE),
			key = BLOG_OPERATE_LIKE_KEY
	))
	public void likeAddRankHotScore(BlogEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) {
			log.debug("重复事件被拒绝: {}", event.getIdempotentKey());
			return;
		}
		log.debug("{} 被点赞，重算热榜", event.getBlogId());
		rankService.recalculateBlogHotScore(event.getBlogId());
		rankService.updateAuthorActivityScore(event.getAuthorId(), 3.0);
	}

}

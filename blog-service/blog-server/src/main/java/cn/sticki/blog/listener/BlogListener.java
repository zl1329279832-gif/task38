package cn.sticki.blog.listener;

import cn.sticki.blog.sdk.BlogEvent;
import cn.sticki.blog.sdk.BlogMqConstants;
import cn.sticki.blog.service.RankService;
import cn.sticki.blog.service.impl.BlogStatsCacheService;
import cn.sticki.common.amqp.autoconfig.EventIdempotencyService;
import cn.sticki.common.amqp.compensation.CompensationTaskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

import static cn.sticki.blog.sdk.BlogMqConstants.*;

/**
 * 监听Blog模块的消息
 * <p>
 * todo 取消的动作也是需要减分的，不过这里应该可以优化，直接用通配符匹配这一个系列的key，根据不同的行为进行分数操作
 *
 * @author durance
 * @version 1.0
 * @date 2022/10/5 14:20
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

	@Resource
	private BlogStatsCacheService blogStatsCacheService;

	@Resource
	private CompensationTaskService compensationTaskService;

	/**
	 * 用户浏览博客对博客热度进行增加
	 *
	 * @param event 博客事件
	 */
	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = BLOG_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = SEE_RANK_QUEUE),
			key = BLOG_OPERATE_READ_KEY
	))
	public void seeAddRankHotScore(BlogEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		log.debug("{} 被浏览热度加1", event.getBlogId());
		try {
			blogStatsCacheService.invalidate(event.getBlogId());
			rankService.recalculateBlogHotScore(event.getBlogId());
			rankService.updateAuthorActivityScore(event.getAuthorId(), 1.0);
		} catch (RuntimeException e) {
			compensationTaskService.saveForRetry(event, SEE_RANK_QUEUE, e);
			throw e;
		}
	}

	/**
	 * 用户收藏博客对博客热度进行增加
	 *
	 * @param event 博客事件
	 */
	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = BLOG_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = COLLECT_RANK_QUEUE),
			key = BLOG_OPERATE_COLLECT_KEY
	))
	public void collectAddRankHotScore(BlogEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		log.debug("{} 被收藏热度加3", event.getBlogId());
		// 使博客统计缓存失效，确保重算时使用最新数据
		blogStatsCacheService.invalidate(event.getBlogId());
		// 重新计算博客热榜分数（含时间衰减和风控）
		rankService.recalculateBlogHotScore(event.getBlogId());
		// 作者活跃度加3
		rankService.updateAuthorActivityScore(event.getAuthorId(), 3.0);
	}

	/**
	 * 用户点赞博客对博客热度进行增加
	 *
	 * @param event 博客事件
	 */
	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = BLOG_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = LIKE_RANK_QUEUE),
			key = BLOG_OPERATE_LIKE_KEY
	))
	public void likeAddRankHotScore(BlogEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		log.debug("{} 被点赞热度加3", event.getBlogId());
		// 使博客统计缓存失效，确保重算时使用最新数据
		blogStatsCacheService.invalidate(event.getBlogId());
		// 重新计算博客热榜分数（含时间衰减和风控）
		rankService.recalculateBlogHotScore(event.getBlogId());
		// 作者活跃度加3
		rankService.updateAuthorActivityScore(event.getAuthorId(), 3.0);
	}

}

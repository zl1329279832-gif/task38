package cn.sticki.user.listener;

import cn.sticki.comment.sdk.CommentEvent;
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

import static cn.sticki.comment.sdk.MqConstants.BLOG_COMMENT_DECREASE_KEY;
import static cn.sticki.comment.sdk.MqConstants.BLOG_COMMENT_INCREASE_KEY;
import static cn.sticki.comment.sdk.MqConstants.COMMENT_TOPIC_EXCHANGE;

/**
 * 监听Comment模块的消息，更新用户评论统计数据
 * <p>
 * 使用 CommentEvent（继承 BaseEvent）替代旧的 CommentDTO，
 * 通过 EventIdempotencyService 保证事件幂等性，防止重复投递导致评论计数错误。
 */
@Slf4j
@Component
public class CommentListener {

	public static final String USER_COMMENT_QUEUE = "user.operate.comment";

	public static final String USER_COMMENT_QUEUE_CANCEL = "user.operate.comment.cancel";

	@Resource
	private UserGeneralMapper userGeneralMapper;

	@Resource
	private EventIdempotencyService eventIdempotencyService;

	@Resource
	private CompensationTaskService compensationTaskService;

	/**
	 * 用户评论博客 - 评论数+1
	 */
	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = COMMENT_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = USER_COMMENT_QUEUE),
			key = BLOG_COMMENT_INCREASE_KEY
	))
	public void commentAddUserGeneral(CommentEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		log.debug("用户 {} 评论加1", event.getAuthorId());
		try {
			userGeneralMapper.updateCommentNumByUserId(event.getAuthorId(), 1);
		} catch (RuntimeException e) {
			compensationTaskService.saveForRetry(event, USER_COMMENT_QUEUE, e);
			throw e;
		}
	}

	/**
	 * 用户删除评论 - 评论数-1（评论删除回滚）
	 */
	@RabbitListener(bindings = @QueueBinding(
			exchange = @Exchange(name = COMMENT_TOPIC_EXCHANGE, type = ExchangeTypes.TOPIC),
			value = @Queue(name = USER_COMMENT_QUEUE_CANCEL),
			key = BLOG_COMMENT_DECREASE_KEY
	))
	public void commentReduceUserGeneral(CommentEvent event) {
		if (!eventIdempotencyService.tryConsume(event)) return;
		log.debug("用户 {} 评论 -1", event.getAuthorId());
		userGeneralMapper.updateCommentNumByUserId(event.getAuthorId(), -1);
	}

}

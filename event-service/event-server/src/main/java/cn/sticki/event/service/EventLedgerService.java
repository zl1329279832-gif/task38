package cn.sticki.event.service;

import cn.sticki.common.amqp.autoconfig.EventIdempotencyService;
import cn.sticki.common.amqp.compensation.CompensationTaskService;
import cn.sticki.common.amqp.event.BaseEvent;
import cn.sticki.event.entity.EventLedgerEntry;
import cn.sticki.event.mapper.EventLedgerMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EventLedgerService {

	private static final String DIRTY_SET_KEY = "snapshot:dirty";

	@Resource
	private EventLedgerMapper eventLedgerMapper;

	@Resource
	private EventIdempotencyService eventIdempotencyService;

	@Resource
	private CompensationTaskService compensationTaskService;

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * 将事件写入账本。
	 * 返回 true 表示成功写入，false 表示被幂等拒绝或重复。
	 */
	public boolean writeLedger(BaseEvent event, String sourceService, Set<Integer> relatedUserIds) {
		if (!eventIdempotencyService.tryConsume(event)) {
			log.debug("事件被幂等拒绝: type={}, key={}", event.getEventType(), event.getIdempotentKey());
			return false;
		}

		EventLedgerEntry entry = new EventLedgerEntry();
		entry.setEventId(event.getEventId());
		entry.setEventType(event.getEventType());
		entry.setEventVersion(event.getEventVersion());
		entry.setIdempotentKey(event.getIdempotentKey());
		entry.setSourceService(sourceService);
		entry.setTimestamp(event.getTimestamp());
		entry.setCreatedAt(LocalDateTime.now());

		try {
			entry.setEventPayload(objectMapper.writeValueAsString(event));
		} catch (Exception e) {
			log.error("事件序列化失败: type={}", event.getEventType(), e);
			return false;
		}

		String userIdsStr = relatedUserIds.stream()
				.sorted()
				.map(String::valueOf)
				.collect(Collectors.joining(","));
		entry.setRelatedUserIds(userIdsStr);

		try {
			eventLedgerMapper.insert(entry);
		} catch (DuplicateKeyException e) {
			log.debug("事件重复写入（DB层幂等）: key={}", event.getIdempotentKey());
			return false;
		} catch (Exception e) {
			log.error("事件写入账本失败: type={}, key={}", event.getEventType(), event.getIdempotentKey(), e);
			compensationTaskService.saveForRetry(event, "event.ledger.write", e);
			return false;
		}

		// 标记相关用户需要重建快照
		for (Integer uid : relatedUserIds) {
			stringRedisTemplate.opsForSet().add(DIRTY_SET_KEY, String.valueOf(uid));
		}

		log.debug("事件写入账本: type={}, key={}, users={}", event.getEventType(), event.getIdempotentKey(), userIdsStr);
		return true;
	}

	/**
	 * 从事件中提取关联用户 ID（通用方法，由 listener 按事件类型补充）
	 */
	public static Set<Integer> toUserIdSet(Integer... userIds) {
		Set<Integer> set = new LinkedHashSet<>();
		for (Integer id : userIds) {
			if (id != null) {
				set.add(id);
			}
		}
		return set;
	}

	public EventLedgerEntry getByIdempotentKey(String key) {
		return eventLedgerMapper.selectOne(
				new LambdaQueryWrapper<EventLedgerEntry>()
						.eq(EventLedgerEntry::getIdempotentKey, key)
		);
	}
}

package cn.sticki.event.service;

import cn.sticki.common.amqp.autoconfig.EventIdempotencyService;
import cn.sticki.common.amqp.compensation.CompensationTaskService;
import cn.sticki.common.amqp.event.BaseEvent;
import cn.sticki.event.entity.EventLedgerEntry;
import cn.sticki.event.mapper.EventLedgerMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EventLedgerService {

	private static final String DIRTY_SET_KEY = "snapshot:dirty";

	private final EventLedgerMapper eventLedgerMapper;
	private final EventIdempotencyService eventIdempotencyService;
	private final CompensationTaskService compensationTaskService;
	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public EventLedgerService(EventLedgerMapper eventLedgerMapper,
	                          EventIdempotencyService eventIdempotencyService,
	                          CompensationTaskService compensationTaskService,
	                          StringRedisTemplate stringRedisTemplate) {
		this.eventLedgerMapper = eventLedgerMapper;
		this.eventIdempotencyService = eventIdempotencyService;
		this.compensationTaskService = compensationTaskService;
		this.stringRedisTemplate = stringRedisTemplate;
	}

	public boolean writeLedger(BaseEvent event, String sourceService, Set<Integer> userIds) {
		if (!eventIdempotencyService.tryConsume(event)) {
			return false;
		}

		EventLedgerEntry entry = new EventLedgerEntry();
		entry.setEventId(event.getEventId());
		entry.setEventType(event.getEventType());
		entry.setEventVersion(1);
		entry.setIdempotentKey(event.getIdempotentKey());
		entry.setSourceService(sourceService);
		entry.setTimestamp(event.getTimestamp());
		entry.setCreatedAt(LocalDateTime.now());

		try {
			entry.setEventPayload(objectMapper.writeValueAsString(event));
		} catch (Exception e) {
			log.error("Failed to serialize event payload", e);
			return false;
		}

		String relatedUserIds = userIds.stream()
				.sorted()
				.map(String::valueOf)
				.collect(Collectors.joining(","));
		entry.setRelatedUserIds(relatedUserIds);

		try {
			eventLedgerMapper.insert(entry);
		} catch (DuplicateKeyException e) {
			log.debug("Duplicate event ledger entry: idempotentKey={}", event.getIdempotentKey());
			return false;
		} catch (Exception e) {
			log.error("Failed to write event ledger entry", e);
			compensationTaskService.saveForRetry(event, "event.ledger.write", e);
			return false;
		}

		String[] userIdStrings = userIds.stream().map(String::valueOf).toArray(String[]::new);
		stringRedisTemplate.opsForSet().add(DIRTY_SET_KEY, userIdStrings);

		return true;
	}

	public static Set<Integer> toUserIdSet(Integer... ids) {
		return Arrays.stream(ids)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	public EventLedgerEntry getByIdempotentKey(String key) {
		return eventLedgerMapper.selectOne(
				new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EventLedgerEntry>()
						.eq(EventLedgerEntry::getIdempotentKey, key)
		);
	}

}

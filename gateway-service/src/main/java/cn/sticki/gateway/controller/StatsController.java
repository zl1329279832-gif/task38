package cn.sticki.gateway.controller;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/gateway/stats")
public class StatsController {

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	private static final String STATS_PV_PREFIX = "gateway:stats:pv:";
	private static final String STATS_UV_PREFIX = "gateway:stats:uv:";
	private static final String STATS_API_PREFIX = "gateway:stats:api:";

	@GetMapping("/pv")
	public Long getTodayPv() {
		String date = LocalDate.now().toString();
		String val = stringRedisTemplate.opsForValue().get(STATS_PV_PREFIX + date);
		return val != null ? Long.parseLong(val) : 0L;
	}

	@GetMapping("/uv")
	public Long getTodayUv() {
		String date = LocalDate.now().toString();
		return stringRedisTemplate.opsForHyperLogLog().size(STATS_UV_PREFIX + date);
	}

	@GetMapping("/api/top")
	public List<Map<String, Object>> getTopApis(@RequestParam(defaultValue = "20") int limit) {
		String date = LocalDate.now().toString();
		Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
				.reverseRangeWithScores(STATS_API_PREFIX + date, 0, limit - 1);
		List<Map<String, Object>> result = new ArrayList<>();
		if (tuples != null) {
			for (ZSetOperations.TypedTuple<String> tuple : tuples) {
				Map<String, Object> item = new LinkedHashMap<>();
				item.put("uri", tuple.getValue());
				item.put("count", tuple.getScore());
				result.add(item);
			}
		}
		return result;
	}

}

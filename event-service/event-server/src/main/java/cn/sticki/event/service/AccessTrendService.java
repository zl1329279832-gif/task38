package cn.sticki.event.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
public class AccessTrendService {

	private static final String STATS_PV_PREFIX = "gateway:stats:pv:";
	private static final String STATS_UV_PREFIX = "gateway:stats:uv:";

	private final StringRedisTemplate stringRedisTemplate;

	public AccessTrendService(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	public Map<String, Object> getAccessTrends(int userId, int days) {
		List<Map<String, Object>> dailyStats = new ArrayList<>();
		long totalPv = 0;
		long totalUv = 0;

		for (int i = days - 1; i >= 0; i--) {
			String date = LocalDate.now().minusDays(i).toString();

			long pv = 0;
			String pvValue = stringRedisTemplate.opsForValue().get(STATS_PV_PREFIX + date);
			if (pvValue != null) {
				pv = Long.parseLong(pvValue);
			}

			long uv = stringRedisTemplate.opsForHyperLogLog().size(STATS_UV_PREFIX + date);

			totalPv += pv;
			totalUv += uv;

			Map<String, Object> dayStat = new LinkedHashMap<>();
			dayStat.put("date", date);
			dayStat.put("pv", pv);
			dayStat.put("uv", uv);
			dailyStats.add(dayStat);
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("dailyStats", dailyStats);
		result.put("totalPv", totalPv);
		result.put("totalUv", totalUv);
		return result;
	}

}

package cn.sticki.event.service;

import jakarta.annotation.Resource;
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

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	/**
	 * 获取用户博客的访问趋势统计
	 * 当前从 Redis 的全局 PV/UV 统计中获取（网关层面记录）
	 * 后续可细化为按用户维度的统计
	 */
	public Map<String, Object> getAccessTrends(int userId, int days) {
		Map<String, Object> result = new LinkedHashMap<>();
		List<Map<String, Object>> dailyStats = new ArrayList<>();
		long totalPv = 0;
		long totalUv = 0;

		for (int i = days - 1; i >= 0; i--) {
			String date = LocalDate.now().minusDays(i).toString();
			String pvKey = STATS_PV_PREFIX + date;
			String uvKey = STATS_UV_PREFIX + date;

			String pvStr = stringRedisTemplate.opsForValue().get(pvKey);
			long pv = pvStr != null ? Long.parseLong(pvStr) : 0;
			long uv = stringRedisTemplate.opsForHyperLogLog().size(uvKey);

			Map<String, Object> dayStat = new LinkedHashMap<>();
			dayStat.put("date", date);
			dayStat.put("pv", pv);
			dayStat.put("uv", uv);
			dailyStats.add(dayStat);

			totalPv += pv;
			totalUv += uv;
		}

		result.put("dailyStats", dailyStats);
		result.put("totalPv", totalPv);
		result.put("totalUv", totalUv);
		return result;
	}
}

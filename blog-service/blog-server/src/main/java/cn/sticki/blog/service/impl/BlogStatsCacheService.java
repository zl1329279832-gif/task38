package cn.sticki.blog.service.impl;

import cn.sticki.blog.mapper.BlogGeneralMapper;
import cn.sticki.blog.mapper.BlogMapper;
import cn.sticki.blog.pojo.bo.BlogStats;
import cn.sticki.blog.pojo.domain.Blog;
import cn.sticki.blog.pojo.domain.BlogGeneral;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class BlogStatsCacheService {

	private static final String KEY_PREFIX = "blog:stats:";

	private static final long TTL_SECONDS = 7200;

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@Resource
	private BlogMapper blogMapper;

	@Resource
	private BlogGeneralMapper blogGeneralMapper;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public BlogStats getBlogStats(Integer blogId) {
		String key = KEY_PREFIX + blogId;
		String cached = stringRedisTemplate.opsForValue().get(key);
		if (cached != null) {
			try {
				return objectMapper.readValue(cached, BlogStats.class);
			} catch (Exception e) {
				log.warn("反序列化BlogStats失败: {}", e.getMessage());
			}
		}
		// Cache miss - load from DB
		Blog blog = blogMapper.selectById(blogId);
		BlogGeneral general = blogGeneralMapper.selectById(blogId);
		if (blog == null) {
			// Cache null to prevent penetration
			stringRedisTemplate.opsForValue().set(key, "", TTL_SECONDS, TimeUnit.SECONDS);
			return null;
		}
		BlogStats stats = new BlogStats();
		stats.setBlogId(blogId);
		if (general != null) {
			stats.setViewCount(general.getViewNum());
			stats.setLikeCount(general.getLikeNum());
			stats.setCollectCount(general.getCollectionNum());
			stats.setCommentCount(general.getCommentNum());
		}
		stats.setPublishTimestamp(blog.getReleaseTime() != null ? blog.getReleaseTime().getTime() : System.currentTimeMillis());
		try {
			stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(stats), TTL_SECONDS, TimeUnit.SECONDS);
		} catch (Exception e) {
			log.warn("序列化BlogStats失败: {}", e.getMessage());
		}
		return stats;
	}

	/**
	 * 使缓存失效（当博客数据更新时调用）
	 */
	public void invalidate(Integer blogId) {
		stringRedisTemplate.delete(KEY_PREFIX + blogId);
	}

}

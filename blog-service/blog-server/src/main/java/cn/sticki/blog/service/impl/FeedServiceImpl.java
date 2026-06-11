package cn.sticki.blog.service.impl;

import cn.sticki.blog.mapper.BlogMapper;
import cn.sticki.blog.pojo.domain.Blog;
import cn.sticki.blog.pojo.vo.FeedItemVO;
import cn.sticki.blog.service.FeedService;
import cn.sticki.user.client.UserClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static cn.sticki.blog.constants.RedisConstants.*;

@Slf4j
@Service
public class FeedServiceImpl implements FeedService {

	@Resource
	private RedisTemplate<String, Object> redisTemplate;

	@Resource
	private UserClient userClient;

	@Resource
	private BlogMapper blogMapper;

	@Override
	public void pushBlogToFollowers(Integer authorId, Integer blogId, long timestampSeconds) {
		cn.sticki.common.result.RestResult<List<Integer>> fansResult = userClient.getFansIdList(authorId);
		List<Integer> fansIdList = (fansResult != null && Boolean.TRUE.equals(fansResult.getStatus())) ? fansResult.getData() : null;
		if (fansIdList == null || fansIdList.isEmpty()) return;
		String member = "blog:" + blogId;
		for (Integer fansId : fansIdList) {
			String key = FEED_KEY_PREFIX + fansId;
			redisTemplate.opsForZSet().add(key, member, timestampSeconds);
			trimFeed(key);
			redisTemplate.expire(key, FEED_TTL, TimeUnit.SECONDS);
		}
	}

	@Override
	public void pushBlinkToFollowers(Integer authorId, Integer blinkId, long timestampSeconds) {
		cn.sticki.common.result.RestResult<List<Integer>> fansResult = userClient.getFansIdList(authorId);
		List<Integer> fansIdList = (fansResult != null && Boolean.TRUE.equals(fansResult.getStatus())) ? fansResult.getData() : null;
		if (fansIdList == null || fansIdList.isEmpty()) return;
		String member = "blink:" + blinkId;
		for (Integer fansId : fansIdList) {
			String key = FEED_KEY_PREFIX + fansId;
			redisTemplate.opsForZSet().add(key, member, timestampSeconds);
			trimFeed(key);
			redisTemplate.expire(key, FEED_TTL, TimeUnit.SECONDS);
		}
	}

	@Override
	public void backfillOnFollow(Integer fansId, Integer followId) {
		LambdaQueryWrapper<Blog> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(Blog::getAuthorId, followId)
				.eq(Blog::getStatus, 1)
				.orderByDesc(Blog::getReleaseTime)
				.last("limit 50");
		List<Blog> blogs = blogMapper.selectList(wrapper);
		if (blogs == null || blogs.isEmpty()) return;
		String key = FEED_KEY_PREFIX + fansId;
		for (Blog blog : blogs) {
			long score = blog.getReleaseTime() != null ? blog.getReleaseTime().getTime() / 1000 : System.currentTimeMillis() / 1000;
			redisTemplate.opsForZSet().add(key, "blog:" + blog.getId(), score);
		}
		trimFeed(key);
		redisTemplate.expire(key, FEED_TTL, TimeUnit.SECONDS);
	}

	@Override
	public void cleanupOnUnfollow(Integer fansId, Integer followId) {
		LambdaQueryWrapper<Blog> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(Blog::getAuthorId, followId).select(Blog::getId);
		List<Blog> blogs = blogMapper.selectList(wrapper);
		if (blogs == null || blogs.isEmpty()) return;
		String key = FEED_KEY_PREFIX + fansId;
		for (Blog blog : blogs) {
			redisTemplate.opsForZSet().remove(key, "blog:" + blog.getId());
		}
	}

	@Override
	public List<FeedItemVO> readFeed(Integer userId, int page, int pageSize) {
		String key = FEED_KEY_PREFIX + userId;
		long start = (long) (page - 1) * pageSize;
		long end = start + pageSize - 1;
		Set<ZSetOperations.TypedTuple<Object>> tuples = redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
		if (tuples == null || tuples.isEmpty()) return Collections.emptyList();

		List<Integer> blogIdList = new ArrayList<>();
		List<Integer> blinkIdList = new ArrayList<>();
		List<FeedItemVO> result = new ArrayList<>();

		for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
			String member = String.valueOf(tuple.getValue());
			FeedItemVO item = new FeedItemVO();
			item.setTimestamp(tuple.getScore() != null ? tuple.getScore().longValue() : 0L);
			if (member.startsWith("blog:")) {
				item.setType("blog");
				item.setContentId(Integer.parseInt(member.substring(5)));
				blogIdList.add(item.getContentId());
			} else if (member.startsWith("blink:")) {
				item.setType("blink");
				item.setContentId(Integer.parseInt(member.substring(6)));
				blinkIdList.add(item.getContentId());
			}
			result.add(item);
		}

		// Batch fetch blog details
		Map<Integer, Blog> blogMap = Collections.emptyMap();
		if (!blogIdList.isEmpty()) {
			List<Blog> blogs = blogMapper.selectBatchIds(blogIdList);
			blogMap = new HashMap<>();
			for (Blog blog : blogs) {
				blogMap.put(blog.getId(), blog);
			}
		}

		for (FeedItemVO item : result) {
			if ("blog".equals(item.getType())) {
				item.setDetail(blogMap.get(item.getContentId()));
			}
			// blink details can be populated when BlinkService is available
		}

		return result;
	}

	private void trimFeed(String key) {
		Long size = redisTemplate.opsForZSet().size(key);
		if (size != null && size > FEED_MAX_SIZE) {
			redisTemplate.opsForZSet().removeRange(key, 0, size - FEED_MAX_SIZE - 1);
		}
	}

}

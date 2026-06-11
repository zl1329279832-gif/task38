package cn.sticki.blog.service.impl;

import cn.sticki.blog.mapper.BlogMapper;
import cn.sticki.blog.pojo.domain.Blog;
import cn.sticki.blog.service.FeedService;
import cn.sticki.common.result.RestResult;
import cn.sticki.user.client.UserClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 关注流推模型实现。
 * 发文时将博客 ID 推送到每个粉丝的 Redis ZSet（feed:{userId}），
 * 按时间戳排序，上限 500 条。
 */
@Slf4j
@Service
public class FeedServiceImpl implements FeedService {

	private static final String FEED_KEY_PREFIX = "feed:";
	private static final long FEED_MAX_SIZE = 500;
	private static final long FEED_TTL_DAYS = 30;

	@Resource
	private RedisTemplate<String, Object> redisTemplate;

	@Resource
	private UserClient userClient;

	@Resource
	private BlogMapper blogMapper;

	@Override
	public void pushBlogToFollowers(int authorId, int blogId, long timestamp) {
		RestResult<List<Integer>> result = userClient.getFansIdList(authorId);
		List<Integer> fans = result.getData();
		if (fans == null || fans.isEmpty()) {
			return;
		}

		String blogValue = "blog:" + blogId;
		for (Integer fanId : fans) {
			String feedKey = FEED_KEY_PREFIX + fanId;
			redisTemplate.opsForZSet().add(feedKey, blogValue, (double) timestamp);
			redisTemplate.expire(feedKey, FEED_TTL_DAYS, TimeUnit.DAYS);

			// 超过上限时裁剪最旧的条目
			Long size = redisTemplate.opsForZSet().size(feedKey);
			if (size != null && size > FEED_MAX_SIZE) {
				redisTemplate.opsForZSet().removeRange(feedKey, 0, size - FEED_MAX_SIZE - 1);
			}
		}
	}

	@Override
	public void backfillOnFollow(int userId, int authorId) {
		// 查询作者近期已发布博客
		LambdaQueryWrapper<Blog> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(Blog::getAuthorId, authorId)
				.eq(Blog::getStatus, 1)
				.orderByDesc(Blog::getReleaseTime)
				.last("limit 50");
		List<Blog> blogs = blogMapper.selectList(wrapper);

		if (blogs == null || blogs.isEmpty()) {
			return;
		}

		String feedKey = FEED_KEY_PREFIX + userId;
		for (Blog blog : blogs) {
			double score = blog.getReleaseTime() != null ? blog.getReleaseTime().getTime() : System.currentTimeMillis();
			redisTemplate.opsForZSet().add(feedKey, "blog:" + blog.getId(), score);
		}
		redisTemplate.expire(feedKey, FEED_TTL_DAYS, TimeUnit.DAYS);
	}

	@Override
	public void cleanupOnUnfollow(int userId, int authorId) {
		LambdaQueryWrapper<Blog> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(Blog::getAuthorId, authorId).eq(Blog::getStatus, 1);
		List<Blog> blogs = blogMapper.selectList(wrapper);

		if (blogs == null || blogs.isEmpty()) {
			return;
		}

		String feedKey = FEED_KEY_PREFIX + userId;
		for (Blog blog : blogs) {
			redisTemplate.opsForZSet().remove(feedKey, "blog:" + blog.getId());
		}
	}

}

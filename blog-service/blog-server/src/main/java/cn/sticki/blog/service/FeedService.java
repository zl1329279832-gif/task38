package cn.sticki.blog.service;

import cn.sticki.blog.pojo.vo.FeedItemVO;

import java.util.List;

/**
 * 关注流 Feed 服务
 *
 * @author sticki
 */
public interface FeedService {

	/**
	 * 将博客推送给作者的粉丝
	 *
	 * @param authorId         作者id
	 * @param blogId           博客id
	 * @param timestampSeconds 时间戳（秒）
	 */
	void pushBlogToFollowers(Integer authorId, Integer blogId, long timestampSeconds);

	/**
	 * 将动态推送给作者的粉丝
	 *
	 * @param authorId         作者id
	 * @param blinkId          动态id
	 * @param timestampSeconds 时间戳（秒）
	 */
	void pushBlinkToFollowers(Integer authorId, Integer blinkId, long timestampSeconds);

	/**
	 * 关注时回填已关注用户的历史博客
	 *
	 * @param fansId   粉丝id
	 * @param followId 被关注用户id
	 */
	void backfillOnFollow(Integer fansId, Integer followId);

	/**
	 * 取关时清理已关注用户的Feed
	 *
	 * @param fansId   粉丝id
	 * @param followId 被关注用户id
	 */
	void cleanupOnUnfollow(Integer fansId, Integer followId);

	/**
	 * 读取关注流
	 *
	 * @param userId   用户id
	 * @param page     页码
	 * @param pageSize 每页大小
	 * @return Feed列表
	 */
	List<FeedItemVO> readFeed(Integer userId, int page, int pageSize);

}

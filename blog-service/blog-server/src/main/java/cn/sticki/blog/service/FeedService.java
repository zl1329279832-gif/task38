package cn.sticki.blog.service;

/**
 * 关注流服务接口
 */
public interface FeedService {

	/**
	 * 将博客推送到作者所有粉丝的关注流
	 *
	 * @param authorId  作者ID
	 * @param blogId    博客ID
	 * @param timestamp 发布时间戳
	 */
	void pushBlogToFollowers(int authorId, int blogId, long timestamp);

	/**
	 * 关注时回填：将被关注者的近期博客加入当前用户的关注流
	 *
	 * @param userId   当前用户ID
	 * @param authorId 被关注的作者ID
	 */
	void backfillOnFollow(int userId, int authorId);

	/**
	 * 取关时清理：从当前用户的关注流中移除该作者的博客
	 *
	 * @param userId   当前用户ID
	 * @param authorId 被取关的作者ID
	 */
	void cleanupOnUnfollow(int userId, int authorId);

}

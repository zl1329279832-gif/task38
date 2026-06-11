package cn.sticki.blog.service.impl;

import cn.sticki.blog.mapper.BlogGeneralMapper;
import cn.sticki.blog.mapper.BlogMapper;
import cn.sticki.blog.pojo.bo.BlogStats;
import cn.sticki.blog.pojo.domain.Blog;
import cn.sticki.blog.pojo.domain.BlogGeneral;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 博客统计数据缓存服务，聚合博客的浏览/点赞/收藏/评论数据
 */
@Service
public class BlogStatsCacheService {

	@Resource
	private BlogMapper blogMapper;

	@Resource
	private BlogGeneralMapper blogGeneralMapper;

	/**
	 * 获取博客统计数据
	 *
	 * @param blogId 博客ID
	 * @return 统计数据，不存在则返回 null
	 */
	public BlogStats getBlogStats(int blogId) {
		BlogGeneral general = blogGeneralMapper.selectById(blogId);
		if (general == null) {
			return null;
		}
		Blog blog = blogMapper.selectById(blogId);
		if (blog == null) {
			return null;
		}

		BlogStats stats = new BlogStats();
		stats.setBlogId(blogId);
		stats.setViewCount(general.getViewNum() != null ? general.getViewNum() : 0);
		stats.setLikeCount(general.getLikeNum() != null ? general.getLikeNum() : 0);
		stats.setCollectCount(general.getCollectionNum() != null ? general.getCollectionNum() : 0);
		stats.setCommentCount(general.getCommentNum() != null ? general.getCommentNum() : 0);
		stats.setPublishTimestamp(blog.getReleaseTime() != null ? blog.getReleaseTime().getTime() : blog.getCreateTime().getTime());
		return stats;
	}

}

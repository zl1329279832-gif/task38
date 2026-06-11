package cn.sticki.blog.pojo.bo;

import lombok.Data;

/**
 * 博客统计数据 BO，用于热榜分数计算
 */
@Data
public class BlogStats {

	private Integer blogId;

	private Integer viewCount;

	private Integer likeCount;

	private Integer collectCount;

	private Integer commentCount;

	/**
	 * 博客发布时间戳（毫秒）
	 */
	private Long publishTimestamp;

}

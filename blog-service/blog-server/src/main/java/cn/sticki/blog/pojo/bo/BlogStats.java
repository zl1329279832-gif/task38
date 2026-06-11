package cn.sticki.blog.pojo.bo;

import lombok.Data;

@Data
public class BlogStats {

	private Integer blogId;

	private int viewCount;

	private int likeCount;

	private int collectCount;

	private int commentCount;

	private long publishTimestamp;

}

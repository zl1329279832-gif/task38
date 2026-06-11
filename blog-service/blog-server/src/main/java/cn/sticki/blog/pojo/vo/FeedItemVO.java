package cn.sticki.blog.pojo.vo;

import lombok.Data;

@Data
public class FeedItemVO {

	private String type;

	private Integer contentId;

	private Long timestamp;

	private Object detail;

}

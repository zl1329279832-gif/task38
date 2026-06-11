package cn.sticki.blog.pojo.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.sql.Timestamp;

/**
 * 用户通知实体
 */
@Data
@TableName("notification")
public class Notification {

	@TableId(type = IdType.AUTO)
	private Long id;

	/**
	 * 接收通知的用户ID
	 */
	private Integer userId;

	/**
	 * 触发通知的用户ID
	 */
	private Integer fromUserId;

	/**
	 * 通知类型：like, collect, comment, follow
	 */
	private String type;

	/**
	 * 关联的博客ID（可为空）
	 */
	private Integer blogId;

	/**
	 * 通知内容
	 */
	private String content;

	/**
	 * 是否已读：0未读，1已读
	 */
	private Integer readStatus;

	/**
	 * 创建时间
	 */
	private Timestamp createTime;

}

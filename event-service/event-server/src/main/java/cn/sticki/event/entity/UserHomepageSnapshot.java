package cn.sticki.event.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "user_homepage_snapshot", autoResultMap = true)
public class UserHomepageSnapshot {

	@TableId(type = IdType.AUTO)
	private Long id;
	private Integer userId;
	private Long snapshotVersion;

	@TableField(typeHandler = JacksonTypeHandler.class)
	private List<Map<String, Object>> followDynamics;

	@TableField(typeHandler = JacksonTypeHandler.class)
	private List<Map<String, Object>> blogInteractions;

	@TableField(typeHandler = JacksonTypeHandler.class)
	private List<Map<String, Object>> commentReplies;

	@TableField(typeHandler = JacksonTypeHandler.class)
	private List<Map<String, Object>> resourceUploads;

	@TableField(typeHandler = JacksonTypeHandler.class)
	private Map<String, Object> accessTrends;

	@TableField(typeHandler = JacksonTypeHandler.class)
	private Map<String, Object> statsSummary;

	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}

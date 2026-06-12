package cn.sticki.event.client.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class HomepageVO {

	private Integer userId;
	private List<FollowDynamicItem> followDynamics;
	private List<BlogInteractionItem> blogInteractions;
	private List<CommentReplyItem> commentReplies;
	private List<ResourceUploadItem> resourceUploads;
	private AccessTrendVO accessTrends;
	private Map<String, Object> statsSummary;

	@Data
	@NoArgsConstructor
	public static class AccessTrendVO {
		private List<DailyStat> dailyStats;
		private long totalPv;
		private long totalUv;
	}

	@Data
	@NoArgsConstructor
	public static class DailyStat {
		private String date;
		private long pv;
		private long uv;
	}

	@Data
	@NoArgsConstructor
	public static class BlogInteractionItem {
		private Integer blogId;
		private String interactionType;
		private Integer actorUserId;
		private long timestamp;
	}

	@Data
	@NoArgsConstructor
	public static class CommentReplyItem {
		private Integer commentId;
		private Integer blogId;
		private Integer userId;
		private String content;
		private long timestamp;
	}

	@Data
	@NoArgsConstructor
	public static class FollowDynamicItem {
		private Integer targetUserId;
		private boolean followed;
		private long timestamp;
	}

	@Data
	@NoArgsConstructor
	public static class ResourceUploadItem {
		private String resourceUrl;
		private String resourceType;
		private long timestamp;
	}

}

package cn.sticki.resource.sdk;

import cn.sticki.common.amqp.event.BaseEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 资源操作事件
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ResourceEvent extends BaseEvent {

	private Integer userId;
	private String resourceUrl;
	private String resourceType;

	public static ResourceEvent ofUpload(Integer userId, String resourceUrl, String resourceType) {
		ResourceEvent event = new ResourceEvent();
		event.setEventId(UUID.randomUUID().toString());
		event.setTimestamp(System.currentTimeMillis());
		event.setEventType(ResourceMqConstants.RESOURCE_UPLOAD_KEY);
		String urlHash = Integer.toHexString(resourceUrl.hashCode());
		event.setIdempotentKey("resource:upload:" + userId + ":" + urlHash);
		event.setUserId(userId);
		event.setResourceUrl(resourceUrl);
		event.setResourceType(resourceType);
		return event;
	}
}

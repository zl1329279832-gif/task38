package cn.sticki.event.client;

import cn.sticki.common.result.RestResult;
import cn.sticki.event.client.dto.HomepageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EventClientResolver implements EventClient {

	@Override
	public RestResult<HomepageVO> getUserHomepage(Integer userId) {
		log.warn("EventClient fallback: getUserHomepage failed for userId={}", userId);
		return null;
	}

}

package cn.sticki.event.client;

import cn.sticki.common.result.RestResult;
import cn.sticki.event.client.dto.HomepageVO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventClientResolver implements EventClient {

	@Override
	public RestResult<HomepageVO> getUserHomepage(Integer userId) {
		log.error("Event 服务异常：getUserHomepage 请求失败, userId={}", userId);
		return RestResult.fail("request fail");
	}
}

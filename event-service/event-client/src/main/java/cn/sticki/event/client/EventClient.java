package cn.sticki.event.client;

import cn.sticki.common.result.RestResult;
import cn.sticki.event.client.dto.HomepageVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "event-server", fallback = EventClientResolver.class)
public interface EventClient {

	@GetMapping("/event/homepage")
	RestResult<HomepageVO> getUserHomepage(@RequestParam Integer userId);

}

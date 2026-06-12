package cn.sticki.event.client;

import cn.sticki.common.result.RestResult;
import cn.sticki.event.client.dto.HomepageVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Import(EventClientResolver.class)
@FeignClient(value = "event-server", fallback = EventClientResolver.class)
public interface EventClient {

	@GetMapping("/homepage/{userId}")
	RestResult<HomepageVO> getUserHomepage(@PathVariable("userId") Integer userId);
}

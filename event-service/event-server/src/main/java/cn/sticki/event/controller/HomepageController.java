package cn.sticki.event.controller;

import cn.sticki.common.result.RestResult;
import cn.sticki.common.web.auth.AuthHelper;
import cn.sticki.event.client.dto.HomepageVO;
import cn.sticki.event.service.SnapshotService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/event")
public class HomepageController {

	private final SnapshotService snapshotService;

	public HomepageController(SnapshotService snapshotService) {
		this.snapshotService = snapshotService;
	}

	@GetMapping("/homepage/me")
	public RestResult<HomepageVO> getMyHomepage() {
		int userId = AuthHelper.getCurrentUserIdOrExit();
		HomepageVO vo = snapshotService.getSnapshot(userId);
		return RestResult.ok(vo);
	}

	@GetMapping("/homepage")
	public RestResult<HomepageVO> getUserHomepage(@RequestParam int userId) {
		HomepageVO vo = snapshotService.getSnapshot(userId);

		Integer currentUserId = AuthHelper.getCurrentUserId();
		if (currentUserId == null || currentUserId != userId) {
			vo.setAccessTrends(null);
		}

		return RestResult.ok(vo);
	}

	@PostMapping("/homepage/rebuild")
	public RestResult<Void> triggerRebuild() {
		int userId = AuthHelper.getCurrentUserIdOrExit();
		snapshotService.rebuildSnapshot(userId);
		return RestResult.ok(null);
	}

}

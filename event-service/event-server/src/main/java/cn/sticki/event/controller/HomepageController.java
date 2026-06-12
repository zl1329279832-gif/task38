package cn.sticki.event.controller;

import cn.sticki.common.result.RestResult;
import cn.sticki.common.web.auth.AuthHelper;
import cn.sticki.event.client.dto.HomepageVO;
import cn.sticki.event.service.SnapshotService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/homepage")
public class HomepageController {

	@Resource
	private SnapshotService snapshotService;

	/**
	 * 获取当前登录用户的主页
	 */
	@GetMapping("/me")
	public RestResult<HomepageVO> getMyHomepage() {
		int userId = AuthHelper.getCurrentUserIdOrExit();
		return RestResult.ok(snapshotService.getSnapshot(userId));
	}

	/**
	 * 获取指定用户的主页（公开，但非本人隐藏访问趋势）
	 */
	@GetMapping("/{userId}")
	public RestResult<HomepageVO> getUserHomepage(@PathVariable int userId) {
		HomepageVO snapshot = snapshotService.getSnapshot(userId);

		// 权限隔离：非本人或未登录则隐藏访问趋势
		Integer currentUserId = AuthHelper.getCurrentUserId();
		if (currentUserId == null || currentUserId != userId) {
			snapshot.setAccessTrends(null);
		}

		return RestResult.ok(snapshot);
	}

	/**
	 * 手动触发当前用户的快照重建
	 */
	@PostMapping("/rebuild")
	public RestResult<Void> triggerRebuild() {
		int userId = AuthHelper.getCurrentUserIdOrExit();
		snapshotService.rebuildSnapshot(userId);
		return RestResult.ok(null);
	}
}

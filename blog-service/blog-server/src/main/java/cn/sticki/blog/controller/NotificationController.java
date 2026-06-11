package cn.sticki.blog.controller;

import cn.sticki.blog.pojo.vo.NotificationVO;
import cn.sticki.blog.service.NotificationService;
import cn.sticki.common.web.auth.AuthHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/notification")
@Validated
public class NotificationController {

	private final int pageSize = 20;

	@Resource
	private NotificationService notificationService;

	@GetMapping("/list")
	public List<NotificationVO> getNotifications(@RequestParam(defaultValue = "1") int page) {
		Integer userId = AuthHelper.getCurrentUserIdOrExit();
		return notificationService.getNotifications(userId, page, pageSize);
	}

	@GetMapping("/unread/count")
	public Long getUnreadCount() {
		Integer userId = AuthHelper.getCurrentUserIdOrExit();
		return notificationService.getUnreadCount(userId);
	}

	@PostMapping("/read/all")
	public void markAllAsRead() {
		Integer userId = AuthHelper.getCurrentUserIdOrExit();
		notificationService.markAllAsRead(userId);
	}

	@PostMapping("/read/{id}")
	public void markAsRead(@PathVariable Long id) {
		Integer userId = AuthHelper.getCurrentUserIdOrExit();
		notificationService.markAsRead(id, userId);
	}

}

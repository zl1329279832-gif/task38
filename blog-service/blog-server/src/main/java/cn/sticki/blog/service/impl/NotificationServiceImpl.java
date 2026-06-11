package cn.sticki.blog.service.impl;

import cn.sticki.blog.mapper.NotificationMapper;
import cn.sticki.blog.pojo.domain.Notification;
import cn.sticki.blog.service.NotificationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 用户通知服务实现
 */
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

	@Resource
	private NotificationMapper notificationMapper;

	@Override
	public long getUnreadCount(int userId) {
		return notificationMapper.countUnread(userId);
	}

	@Override
	public void markAllAsRead(int userId) {
		notificationMapper.markAllAsRead(userId);
	}

	@Override
	public void markAsRead(long notificationId, int userId) {
		lambdaUpdate()
				.eq(Notification::getId, notificationId)
				.eq(Notification::getUserId, userId)
				.set(Notification::getReadStatus, 1)
				.update();
	}

}

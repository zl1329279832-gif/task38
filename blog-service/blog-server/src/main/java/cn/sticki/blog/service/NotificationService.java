package cn.sticki.blog.service;

/**
 * 用户通知服务接口
 */
public interface NotificationService {

	/**
	 * 获取用户未读通知数量
	 *
	 * @param userId 用户ID
	 * @return 未读数量
	 */
	long getUnreadCount(int userId);

	/**
	 * 将用户所有通知标记为已读
	 *
	 * @param userId 用户ID
	 */
	void markAllAsRead(int userId);

	/**
	 * 标记单条通知为已读
	 *
	 * @param notificationId 通知ID
	 * @param userId         用户ID
	 */
	void markAsRead(long notificationId, int userId);

}

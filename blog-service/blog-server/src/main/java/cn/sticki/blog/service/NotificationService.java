package cn.sticki.blog.service;

import cn.sticki.blog.pojo.domain.Notification;
import cn.sticki.blog.pojo.vo.NotificationVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface NotificationService extends IService<Notification> {
    void createNotification(Integer userId, Integer type, String title, String content, Integer targetId, String targetType, Integer senderId);
    List<NotificationVO> getNotifications(Integer userId, int page, int pageSize);
    long getUnreadCount(Integer userId);
    void markAllAsRead(Integer userId);
    void markAsRead(Long notificationId, Integer userId);
}

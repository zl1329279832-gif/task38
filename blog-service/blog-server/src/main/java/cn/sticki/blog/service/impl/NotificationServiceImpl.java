package cn.sticki.blog.service.impl;

import cn.sticki.blog.mapper.NotificationMapper;
import cn.sticki.blog.pojo.domain.Notification;
import cn.sticki.blog.pojo.vo.NotificationVO;
import cn.sticki.blog.service.NotificationService;
import cn.sticki.common.result.RestResult;
import cn.sticki.user.client.UserClient;
import cn.sticki.user.dto.UserDTO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

	@Resource
	private NotificationMapper notificationMapper;

	@Resource
	private UserClient userClient;

	@Override
	public void createNotification(Integer userId, Integer type, String title, String content, Integer targetId, String targetType, Integer senderId) {
		Notification notification = new Notification();
		notification.setUserId(userId);
		notification.setType(type);
		notification.setTitle(title);
		notification.setContent(content);
		notification.setTargetId(targetId);
		notification.setTargetType(targetType);
		notification.setSenderId(senderId);
		notification.setIsRead(0);
		notification.setDeleted(0);
		notification.setCreateTime(new Timestamp(System.currentTimeMillis()));
		save(notification);
	}

	@Override
	public List<NotificationVO> getNotifications(Integer userId, int page, int pageSize) {
		LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(Notification::getUserId, userId);
		wrapper.eq(Notification::getDeleted, 0);
		wrapper.orderByDesc(Notification::getCreateTime);
		Page<Notification> pageParam = new Page<>(page, pageSize);
		Page<Notification> result = notificationMapper.selectPage(pageParam, wrapper);
		List<Notification> records = result.getRecords();
		if (records.isEmpty()) {
			return Collections.emptyList();
		}

		// Collect sender IDs for batch query
		Set<Integer> senderIds = records.stream()
				.map(Notification::getSenderId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		// Batch fetch sender info
		Map<Integer, UserDTO> senderMap = Collections.emptyMap();
		if (!senderIds.isEmpty()) {
			RestResult<Map<Integer, UserDTO>> userResult = userClient.getUserList(senderIds);
			if (userResult.getStatus() && userResult.getData() != null) {
				senderMap = userResult.getData();
			}
		}

		// Map to VO
		List<NotificationVO> voList = new ArrayList<>();
		for (Notification notification : records) {
			NotificationVO vo = new NotificationVO();
			vo.setId(notification.getId());
			vo.setType(notification.getType());
			vo.setTitle(notification.getTitle());
			vo.setContent(notification.getContent());
			vo.setTargetId(notification.getTargetId());
			vo.setTargetType(notification.getTargetType());
			vo.setSenderId(notification.getSenderId());
			vo.setIsRead(notification.getIsRead() != null && notification.getIsRead() == 1);
			vo.setCreateTime(notification.getCreateTime());

			// Enrich with sender info
			if (notification.getSenderId() != null) {
				UserDTO sender = senderMap.get(notification.getSenderId());
				if (sender != null) {
					vo.setSenderNickname(sender.getNickname());
					vo.setSenderAvatar(sender.getAvatarUrl());
				}
			}
			voList.add(vo);
		}
		return voList;
	}

	@Override
	public long getUnreadCount(Integer userId) {
		return notificationMapper.countUnread(userId);
	}

	@Override
	public void markAllAsRead(Integer userId) {
		notificationMapper.markAllAsRead(userId);
	}

	@Override
	public void markAsRead(Long notificationId, Integer userId) {
		lambdaUpdate()
				.set(Notification::getIsRead, 1)
				.eq(Notification::getId, notificationId)
				.eq(Notification::getUserId, userId)
				.update();
	}

}

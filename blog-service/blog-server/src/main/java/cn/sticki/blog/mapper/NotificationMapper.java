package cn.sticki.blog.mapper;

import cn.sticki.blog.pojo.domain.Notification;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 通知 Mapper
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

	/**
	 * 统计用户未读通知数
	 *
	 * @param userId 用户ID
	 * @return 未读通知数量
	 */
	@Select("select count(*) from notification where user_id = #{userId} and read_status = 0")
	long countUnread(int userId);

	/**
	 * 将用户所有通知标记为已读
	 *
	 * @param userId 用户ID
	 * @return 更新行数
	 */
	@Update("update notification set read_status = 1 where user_id = #{userId} and read_status = 0")
	int markAllAsRead(int userId);

}

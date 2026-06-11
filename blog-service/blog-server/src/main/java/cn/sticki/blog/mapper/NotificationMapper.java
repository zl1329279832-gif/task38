package cn.sticki.blog.mapper;

import cn.sticki.blog.pojo.domain.Notification;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    @Select("SELECT COUNT(*) FROM notification WHERE user_id = #{userId} AND is_read = 0 AND deleted = 0")
    long countUnread(@Param("userId") Integer userId);

    @Update("UPDATE notification SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0 AND deleted = 0")
    int markAllAsRead(@Param("userId") Integer userId);
}

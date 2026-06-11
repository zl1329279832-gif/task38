package cn.sticki.blog.pojo.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.sql.Timestamp;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer userId;
    private Integer type;
    private String title;
    private String content;
    private Integer targetId;
    private String targetType;
    private Integer senderId;
    private Integer isRead;
    private Timestamp createTime;
    private Integer deleted;
}

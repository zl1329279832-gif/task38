package cn.sticki.blog.pojo.vo;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class NotificationVO {
    private Long id;
    private Integer type;
    private String title;
    private String content;
    private Integer targetId;
    private String targetType;
    private Integer senderId;
    private String senderNickname;
    private String senderAvatar;
    private Boolean isRead;
    private Timestamp createTime;
}

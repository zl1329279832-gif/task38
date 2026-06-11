CREATE TABLE IF NOT EXISTS notification (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL COMMENT '通知接收人',
  type INT NOT NULL COMMENT '1=关注 2=点赞 3=收藏 4=评论',
  title VARCHAR(200) NOT NULL,
  content VARCHAR(500),
  target_id INT COMMENT '引用内容ID',
  target_type VARCHAR(20) COMMENT 'blog blink user',
  sender_id INT COMMENT '触发通知的用户ID',
  is_read TINYINT DEFAULT 0,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_user_read (user_id, is_read),
  INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

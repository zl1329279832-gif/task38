-- 事件账本数据库
CREATE DATABASE IF NOT EXISTS `event` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `event`;

-- 事件账本表（追加写入，不可修改）
CREATE TABLE `event_ledger` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    `event_id` VARCHAR(64) NOT NULL COMMENT '全局唯一事件ID',
    `event_type` VARCHAR(64) NOT NULL COMMENT '事件类型，如 blog.insert, user.follow',
    `event_version` INT NOT NULL DEFAULT 1 COMMENT '事件模型版本号',
    `idempotent_key` VARCHAR(128) NOT NULL COMMENT '幂等键，保证唯一性',
    `event_payload` TEXT NOT NULL COMMENT 'JSON序列化的完整事件数据',
    `source_service` VARCHAR(32) NOT NULL COMMENT '来源服务：blog/user/comment/blink/resource',
    `related_user_ids` VARCHAR(256) DEFAULT NULL COMMENT '关联用户ID，逗号分隔',
    `timestamp` BIGINT NOT NULL COMMENT '事件原始时间戳（epoch millis）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '写入时间',
    UNIQUE KEY `uk_idempotent` (`idempotent_key`),
    INDEX `idx_type_ts` (`event_type`, `timestamp`),
    INDEX `idx_source_ts` (`source_service`, `timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跨服务互动事件账本';

-- 用户主页聚合快照表
CREATE TABLE `user_homepage_snapshot` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    `user_id` INT NOT NULL COMMENT '用户ID',
    `snapshot_version` BIGINT NOT NULL DEFAULT 0 COMMENT '最后处理的event_ledger.id',
    `follow_dynamics` JSON DEFAULT NULL COMMENT '关注/取关事件摘要',
    `blog_interactions` JSON DEFAULT NULL COMMENT '博客互动事件摘要（点赞/收藏/发布）',
    `comment_replies` JSON DEFAULT NULL COMMENT '评论回复事件摘要',
    `resource_uploads` JSON DEFAULT NULL COMMENT '资源上传事件摘要',
    `access_trends` JSON DEFAULT NULL COMMENT '访问趋势统计',
    `stats_summary` JSON DEFAULT NULL COMMENT '汇总统计数据',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户主页聚合快照';

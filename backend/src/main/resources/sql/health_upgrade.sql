-- ============================================================
-- 个性化饮食健康管理模块 数据库变更脚本
-- 执行方式: mysql -uroot -p < health_upgrade.sql (可重复执行)
-- ============================================================
USE diet_db;

-- 用户表新增目标体重字段(体重管理达标预测用)
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'diet_db' AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'target_weight');
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN target_weight DECIMAL(5,1) DEFAULT NULL COMMENT ''目标体重(kg)'' AFTER weight',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- AI营养问答历史对话表
CREATE TABLE IF NOT EXISTS ai_chat_message (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `user_id`     BIGINT      NOT NULL COMMENT '用户ID',
    `role`        VARCHAR(10) NOT NULL COMMENT '角色: USER-用户提问, ASSISTANT-AI回答',
    `content`     TEXT        NOT NULL COMMENT '消息内容',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE = InnoDB COMMENT = 'AI营养问答历史对话表';
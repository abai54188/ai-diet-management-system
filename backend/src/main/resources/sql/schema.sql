-- ============================================================
-- AI智能饮食管理系统 初始化脚本
-- 执行方式: mysql -uroot -p < schema.sql 或在客户端中直接执行
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS diet_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE diet_db;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`    VARCHAR(20)  NOT NULL COMMENT '用户名(登录账号)',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密存储)',
    `nickname`    VARCHAR(30)  DEFAULT '' COMMENT '用户昵称',
    `role`        VARCHAR(10)  NOT NULL DEFAULT 'USER' COMMENT '角色: USER-普通用户, ADMIN-管理员',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB COMMENT = '用户表';

-- ============================================================
-- 说明: 首次使用请先通过前端注册页创建账号，再将指定账号提升为管理员:
--   UPDATE user SET role = 'ADMIN' WHERE username = '你的账号';
-- ============================================================
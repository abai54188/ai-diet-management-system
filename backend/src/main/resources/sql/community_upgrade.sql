-- ============================================================
-- 社区/看板/管理后台 数据库变更脚本(可重复执行)
-- ============================================================
USE diet_db;

-- sys_user 新增状态字段(0-正常, 1-禁用)
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='diet_db' AND TABLE_NAME='sys_user' AND COLUMN_NAME='status');
SET @ddl = IF(@col_exists=0,
    'ALTER TABLE sys_user ADD COLUMN status TINYINT NOT NULL DEFAULT 0 COMMENT ''状态:0-正常,1-禁用'' AFTER role',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 社区动态评论表
CREATE TABLE IF NOT EXISTS post_comment (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `post_id`     BIGINT      NOT NULL COMMENT '动态ID',
    `user_id`     BIGINT      NOT NULL COMMENT '评论用户ID',
    `content`     VARCHAR(500) NOT NULL COMMENT '评论内容',
    `status`      TINYINT     NOT NULL DEFAULT 0 COMMENT '审核状态:0-正常,1-屏蔽',
    `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`), KEY `idx_post` (`post_id`), KEY `idx_user` (`user_id`)
) ENGINE=InnoDB COMMENT='社区动态评论表';

-- 动态点赞表
CREATE TABLE IF NOT EXISTS post_like (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
    `post_id`     BIGINT   NOT NULL COMMENT '动态ID',
    `user_id`     BIGINT   NOT NULL COMMENT '点赞用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`), UNIQUE KEY `uk_post_user` (`post_id`, `user_id`)
) ENGINE=InnoDB COMMENT='动态点赞表';

-- 用户关注表
CREATE TABLE IF NOT EXISTS user_follow (
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '关注ID',
    `follower_id` BIGINT   NOT NULL COMMENT '关注者ID',
    `followee_id` BIGINT  NOT NULL COMMENT '被关注者ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`), UNIQUE KEY `uk_follow` (`follower_id`, `followee_id`)
) ENGINE=InnoDB COMMENT='用户关注表';

-- 食谱收藏表(社区食谱收藏与分类)
CREATE TABLE IF NOT EXISTS recipe_collect (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
    `user_id`     BIGINT      NOT NULL COMMENT '用户ID',
    `recipe_id`   BIGINT      NOT NULL COMMENT '食谱ID',
    `category`    VARCHAR(20) NOT NULL DEFAULT '默认' COMMENT '收藏分类(自定义文件夹)',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`), UNIQUE KEY `uk_user_recipe` (`user_id`, `recipe_id`)
) ENGINE=InnoDB COMMENT='食谱收藏表';

-- 食谱评分反馈表
CREATE TABLE IF NOT EXISTS recipe_rating (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评分ID',
    `recipe_id`   BIGINT       NOT NULL COMMENT '食谱ID',
    `user_id`     BIGINT       NOT NULL COMMENT '评分用户ID',
    `score`       TINYINT      NOT NULL COMMENT '评分1-5星',
    `feedback`    VARCHAR(500) DEFAULT '' COMMENT '文字反馈',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`), UNIQUE KEY `uk_user_recipe` (`user_id`, `recipe_id`)
) ENGINE=InnoDB COMMENT='食谱评分反馈表';

-- 内容举报表
CREATE TABLE IF NOT EXISTS content_report (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '举报ID',
    `reporter_id`  BIGINT       NOT NULL COMMENT '举报人ID',
    `target_type`  VARCHAR(10)  NOT NULL COMMENT '目标类型: POST-动态, COMMENT-评论',
    `target_id`    BIGINT       NOT NULL COMMENT '目标ID',
    `reason`       VARCHAR(200) NOT NULL COMMENT '举报理由',
    `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态:0-待处理,1-已处理',
    `handle_result` VARCHAR(200) DEFAULT NULL COMMENT '处理结果说明',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `handle_time`  DATETIME     DEFAULT NULL COMMENT '处理时间',
    PRIMARY KEY (`id`), KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='内容举报表';

-- community_post 补充逻辑删除已有, 增加置顶/热度字段
SET @col2 = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='diet_db' AND TABLE_NAME='community_post' AND COLUMN_NAME='is_top');
SET @ddl2 = IF(@col2=0, 'ALTER TABLE community_post ADD COLUMN is_top TINYINT NOT NULL DEFAULT 0 COMMENT ''是否置顶'' AFTER comment_count', 'SELECT 1');
PREPARE stmt2 FROM @ddl2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;
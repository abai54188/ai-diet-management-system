-- ============================================================
-- AI智能饮食管理系统 数据库初始化脚本
-- 依据《中国食物成分表(第6版)》数据规范设计
-- 执行方式: mysql -uroot -p < init.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS diet_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE diet_db;

-- ------------------------------------------------------------
-- 1. 用户表 sys_user
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`       VARCHAR(20)  NOT NULL COMMENT '用户名(登录账号)',
    `password`       VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密存储)',
    `nickname`       VARCHAR(30)  DEFAULT '' COMMENT '用户昵称',
    `role`           VARCHAR(10)  NOT NULL DEFAULT 'USER' COMMENT '角色: USER-普通用户, ADMIN-管理员',
    `height`         DECIMAL(5,1) DEFAULT NULL COMMENT '身高(cm)',
    `weight`         DECIMAL(5,1) DEFAULT NULL COMMENT '体重(kg)',
    `age`            INT          DEFAULT NULL COMMENT '年龄(岁)',
    `gender`         TINYINT      DEFAULT 0 COMMENT '性别: 0-未知, 1-男, 2-女',
    `activity_level` VARCHAR(10)  DEFAULT 'LIGHT' COMMENT '活动量: SEDENTARY-久坐, LIGHT-轻度, MODERATE-中度, HIGH-高度',
    `health_goal`     VARCHAR(10)  DEFAULT 'KEEP' COMMENT '健康目标: LOSE-减脂, KEEP-维持, GAIN-增重',
    `allergy`        VARCHAR(200) DEFAULT '' COMMENT '忌口/过敏源(逗号分隔, 如: 海鲜,花生,麸质)',
    `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB COMMENT = '用户表';

-- ------------------------------------------------------------
-- 2. 食材营养库表 food_nutrition (数据标准: 中国食物成分表第6版, 每100g可食部)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS food_nutrition (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '食材ID',
    `food_name`      VARCHAR(50)  NOT NULL COMMENT '食材名称(国内日常通用叫法)',
    `alias`          VARCHAR(100) DEFAULT '' COMMENT '别名(南北通用叫法, 分号分隔)',
    `category`       VARCHAR(20)  NOT NULL COMMENT '分类: 谷薯类/蔬菜类/水果类/肉禽蛋类/水产类/奶豆类/坚果类/调料类/饮品类/加工食品类',
    `calorie`        DECIMAL(6,1) NOT NULL COMMENT '热量(kcal/100g可食部)',
    `protein`        DECIMAL(6,1) NOT NULL DEFAULT 0 COMMENT '蛋白质(g/100g)',
    `carbohydrate`   DECIMAL(6,1) NOT NULL DEFAULT 0 COMMENT '碳水化合物(g/100g)',
    `fat`            DECIMAL(6,1) NOT NULL DEFAULT 0 COMMENT '脂肪(g/100g)',
    `dietary_fiber`  DECIMAL(6,1) DEFAULT 0 COMMENT '膳食纤维(g/100g)',
    `vitamin_c`      DECIMAL(6,1) DEFAULT 0 COMMENT '维生素C(mg/100g)',
    `vitamin_e`      DECIMAL(6,1) DEFAULT 0 COMMENT '维生素E(mg/100g)',
    `vitamin_b1`     DECIMAL(5,2) DEFAULT 0 COMMENT '硫胺素VB1(mg/100g)',
    `vitamin_b2`     DECIMAL(5,2) DEFAULT 0 COMMENT '核黄素VB2(mg/100g)',
    `calcium`        DECIMAL(7,1) DEFAULT 0 COMMENT '钙(mg/100g)',
    `iron`           DECIMAL(6,1) DEFAULT 0 COMMENT '铁(mg/100g)',
    `sodium`         DECIMAL(7,1) DEFAULT 0 COMMENT '钠(mg/100g)',
    `potassium`      DECIMAL(7,1) DEFAULT 0 COMMENT '钾(mg/100g)',
    `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_food_name` (`food_name`),
    KEY `idx_category` (`category`),
    KEY `idx_food_name` (`food_name`),
    FULLTEXT KEY `ft_food_name` (`food_name`) WITH PARSER ngram
) ENGINE = InnoDB COMMENT = '食材营养库表';

-- ------------------------------------------------------------
-- 3. 用户收藏食材表 user_food_collect
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_food_collect (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
    `user_id`      BIGINT       NOT NULL COMMENT '用户ID',
    `food_id`      BIGINT       NOT NULL COMMENT '食材ID',
    `fixed_weight` DECIMAL(6,1) NOT NULL DEFAULT 100.0 COMMENT '固定重量(g,用户习惯用量)',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_food` (`user_id`, `food_id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB COMMENT = '用户收藏食材表';

-- ------------------------------------------------------------
-- 4. 饮食记录表 diet_record
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS diet_record (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `food_id`     BIGINT       NOT NULL COMMENT '食材ID',
    `food_name`   VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '食材名称(冗余存储, 防止食材库变更影响历史记录)',
    `weight`      DECIMAL(6,1) NOT NULL COMMENT '食用重量(g)',
    `calorie`     DECIMAL(8,1) NOT NULL COMMENT '摄入热量(kcal, 按 weight 计算后落库)',
    `meal_type`   VARCHAR(10)  NOT NULL COMMENT '用餐时段: BREAKFAST-早餐, LUNCH-午餐, DINNER-晚餐, SNACK-加餐',
    `record_date` DATE         NOT NULL COMMENT '记录日期',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_date` (`user_id`, `record_date`),
    KEY `idx_date` (`record_date`)
) ENGINE = InnoDB COMMENT = '饮食记录表';

-- ------------------------------------------------------------
-- 5. 用户健康档案表 health_profile
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS health_profile (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '档案ID',
    `user_id`        BIGINT       NOT NULL COMMENT '用户ID',
    `daily_calorie`  DECIMAL(7,1) NOT NULL COMMENT '每日推荐热量(kcal)',
    `protein_ratio`  DECIMAL(4,1) NOT NULL DEFAULT 20.0 COMMENT '蛋白质供能占比(%)',
    `carb_ratio`     DECIMAL(4,1) NOT NULL DEFAULT 55.0 COMMENT '碳水供能占比(%)',
    `fat_ratio`      DECIMAL(4,1) NOT NULL DEFAULT 25.0 COMMENT '脂肪供能占比(%)',
    `meal_ratio`     VARCHAR(50) NOT NULL DEFAULT '30,40,30' COMMENT '三餐比例(早,午,晚 供能占比%)',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user` (`user_id`)
) ENGINE = InnoDB COMMENT = '用户健康档案表';

-- ------------------------------------------------------------
-- 6. 食谱表 recipe
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS recipe (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '食谱ID',
    `recipe_name`        VARCHAR(50)  NOT NULL COMMENT '食谱名称',
    `cooking_method`     TEXT         COMMENT '做法(步骤说明)',
    `ingredients`        JSON         COMMENT '食材列表(JSON数组: [{foodId,foodName,weight}])',
    `calorie_per_serving` DECIMAL(8,1) DEFAULT NULL COMMENT '单份热量(kcal)',
    `difficulty`         TINYINT      NOT NULL DEFAULT 1 COMMENT '难度: 1-简单, 2-一般, 3-较难',
    `cooking_time`       INT          DEFAULT NULL COMMENT '耗时(分钟)',
    `user_id`            BIGINT       DEFAULT NULL COMMENT '创建人用户ID(官方食谱为NULL)',
    `is_official`        TINYINT     NOT NULL DEFAULT 0 COMMENT '是否官方: 0-用户创建, 1-官方食谱',
    `deleted`            TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    `create_time`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_name` (`recipe_name`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB COMMENT = '食谱表';

-- ------------------------------------------------------------
-- 7. 社区动态表 community_post
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS community_post (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '动态ID',
    `user_id`       BIGINT       NOT NULL COMMENT '发布用户ID',
    `title`         VARCHAR(100) NOT NULL COMMENT '标题',
    `content`       TEXT         COMMENT '正文内容',
    `images`        JSON         COMMENT '图片URL列表(JSON数组)',
    `like_count`    INT          NOT NULL DEFAULT 0 COMMENT '点赞数',
    `comment_count` INT          NOT NULL DEFAULT 0 COMMENT '评论数',
    `publish_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_publish_time` (`publish_time`)
) ENGINE = InnoDB COMMENT = '社区动态表';

-- ------------------------------------------------------------
-- 8. 体重记录表 weight_record
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS weight_record (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `weight`      DECIMAL(5,1) NOT NULL COMMENT '体重(kg)',
    `record_date` DATE         NOT NULL COMMENT '记录日期',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_date` (`user_id`, `record_date`)
) ENGINE = InnoDB COMMENT = '体重记录表';

-- ------------------------------------------------------------
-- 旧 user 表数据迁移至 sys_user (仅首次执行有效, 迁移后删除旧表)
-- ------------------------------------------------------------
INSERT INTO sys_user (username, password, nickname, role)
SELECT u.username, u.password, u.nickname, u.role
FROM `user` u
WHERE NOT EXISTS (SELECT 1 FROM sys_user s WHERE s.username = u.username);

DROP TABLE IF EXISTS `user`;
-- ============================================================
-- AI智能饮食管理系统 - SQLite 数据库初始化脚本
-- 说明:
--   1. 本脚本兼容 SQLite3, 由 Spring Boot 启动时通过 spring.sql.init 自动执行
--   2. 所有表使用 CREATE TABLE IF NOT EXISTS, 可重复执行
--   3. 类型映射: BIGINT -> INTEGER, VARCHAR/TEXT/JSON -> TEXT,
--      DECIMAL -> NUMERIC, TINYINT/INT -> INTEGER, DATE/DATETIME -> TEXT
--   4. 自增主键: INTEGER PRIMARY KEY AUTOINCREMENT
--   5. 时间字段 create_time/update_time:
--      - 具备 @TableField(fill=...) 的实体由 MyBatis-Plus 自动填充
--      - 其余表通过 SQLite DEFAULT datetime('now','localtime') 自动填充(等价 MySQL 的 DEFAULT CURRENT_TIMESTAMP)
-- ============================================================

-- ------------------------------------------------------------
-- 1. 用户表 sys_user
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    username       TEXT    NOT NULL,
    password       TEXT    NOT NULL,
    nickname       TEXT    DEFAULT '',
    role           TEXT    NOT NULL DEFAULT 'USER',
    status         INTEGER NOT NULL DEFAULT 0,
    height         NUMERIC,
    weight         NUMERIC,
    target_weight  NUMERIC,
    age            INTEGER,
    gender         INTEGER DEFAULT 0,
    activity_level TEXT    DEFAULT 'LIGHT',
    health_goal    TEXT    DEFAULT 'KEEP',
    allergy        TEXT    DEFAULT '',
    deleted        INTEGER NOT NULL DEFAULT 0,
    create_time    TEXT    DEFAULT (datetime('now', 'localtime')),
    update_time    TEXT    DEFAULT (datetime('now', 'localtime')),
    UNIQUE (username)
);

-- ------------------------------------------------------------
-- 2. 食材营养库表 food_nutrition (每100g可食部)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS food_nutrition (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    food_name      TEXT    NOT NULL,
    alias          TEXT    DEFAULT '',
    category       TEXT    NOT NULL,
    calorie        NUMERIC NOT NULL,
    protein        NUMERIC NOT NULL DEFAULT 0,
    carbohydrate   NUMERIC NOT NULL DEFAULT 0,
    fat            NUMERIC NOT NULL DEFAULT 0,
    dietary_fiber  NUMERIC DEFAULT 0,
    vitamin_c      NUMERIC DEFAULT 0,
    vitamin_e      NUMERIC DEFAULT 0,
    vitamin_b1     NUMERIC DEFAULT 0,
    vitamin_b2     NUMERIC DEFAULT 0,
    calcium        NUMERIC DEFAULT 0,
    iron           NUMERIC DEFAULT 0,
    sodium         NUMERIC DEFAULT 0,
    potassium      NUMERIC DEFAULT 0,
    deleted        INTEGER NOT NULL DEFAULT 0,
    create_time    TEXT    DEFAULT (datetime('now', 'localtime')),
    update_time    TEXT    DEFAULT (datetime('now', 'localtime')),
    UNIQUE (food_name)
);
CREATE INDEX IF NOT EXISTS idx_food_category ON food_nutrition (category);

-- ------------------------------------------------------------
-- 3. 用户收藏食材表 user_food_collect
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_food_collect (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id      INTEGER NOT NULL,
    food_id      INTEGER NOT NULL,
    fixed_weight NUMERIC NOT NULL DEFAULT 100.0,
    create_time  TEXT    DEFAULT (datetime('now', 'localtime')),
    UNIQUE (user_id, food_id)
);

-- ------------------------------------------------------------
-- 4. 饮食记录表 diet_record
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS diet_record (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id      INTEGER NOT NULL,
    food_id      INTEGER,
    food_name    TEXT    NOT NULL DEFAULT '',
    weight       NUMERIC NOT NULL,
    calorie      NUMERIC NOT NULL,
    protein      NUMERIC,
    carbohydrate NUMERIC,
    fat          NUMERIC,
    meal_type    TEXT    NOT NULL,
    record_date  TEXT    NOT NULL,
    create_time  TEXT    DEFAULT (datetime('now', 'localtime'))
);
CREATE INDEX IF NOT EXISTS idx_diet_user_date ON diet_record (user_id, record_date);

-- ------------------------------------------------------------
-- 5. 用户健康档案表 health_profile
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS health_profile (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id       INTEGER NOT NULL,
    daily_calorie NUMERIC NOT NULL,
    protein_ratio NUMERIC NOT NULL DEFAULT 20.0,
    carb_ratio    NUMERIC NOT NULL DEFAULT 55.0,
    fat_ratio     NUMERIC NOT NULL DEFAULT 25.0,
    meal_ratio    TEXT    NOT NULL DEFAULT '30,40,30',
    create_time   TEXT    DEFAULT (datetime('now', 'localtime')),
    update_time   TEXT    DEFAULT (datetime('now', 'localtime')),
    UNIQUE (user_id)
);

-- ------------------------------------------------------------
-- 6. 食谱表 recipe
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS recipe (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    recipe_name         TEXT    NOT NULL,
    cooking_method      TEXT,
    ingredients         TEXT,
    calorie_per_serving NUMERIC,
    difficulty          INTEGER NOT NULL DEFAULT 1,
    cooking_time        INTEGER,
    user_id             INTEGER,
    is_official         INTEGER NOT NULL DEFAULT 0,
    deleted             INTEGER NOT NULL DEFAULT 0,
    create_time         TEXT    DEFAULT (datetime('now', 'localtime')),
    update_time         TEXT    DEFAULT (datetime('now', 'localtime'))
);

-- ------------------------------------------------------------
-- 7. 社区动态表 community_post
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS community_post (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id      INTEGER NOT NULL,
    title        TEXT    NOT NULL,
    content      TEXT,
    images       TEXT,
    recipe_id    INTEGER,
    like_count   INTEGER NOT NULL DEFAULT 0,
    is_top       INTEGER NOT NULL DEFAULT 0,
    comment_count INTEGER NOT NULL DEFAULT 0,
    publish_time TEXT,
    deleted      INTEGER NOT NULL DEFAULT 0,
    create_time  TEXT    DEFAULT (datetime('now', 'localtime')),
    update_time  TEXT    DEFAULT (datetime('now', 'localtime'))
);

-- ------------------------------------------------------------
-- 8. 体重记录表 weight_record
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS weight_record (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER NOT NULL,
    weight      NUMERIC NOT NULL,
    record_date TEXT    NOT NULL,
    create_time TEXT    DEFAULT (datetime('now', 'localtime')),
    UNIQUE (user_id, record_date)
);

-- ------------------------------------------------------------
-- 9. AI营养问答历史对话表 ai_chat_message
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai_chat_message (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER NOT NULL,
    role        TEXT    NOT NULL,
    content     TEXT    NOT NULL,
    create_time TEXT    DEFAULT (datetime('now', 'localtime'))
);

-- ------------------------------------------------------------
-- 10. 社区动态评论表 post_comment
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS post_comment (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    post_id     INTEGER NOT NULL,
    user_id     INTEGER NOT NULL,
    content     TEXT    NOT NULL,
    status      INTEGER NOT NULL DEFAULT 0,
    deleted     INTEGER NOT NULL DEFAULT 0,
    create_time TEXT    DEFAULT (datetime('now', 'localtime'))
);

-- ------------------------------------------------------------
-- 11. 动态点赞表 post_like
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS post_like (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    post_id     INTEGER NOT NULL,
    user_id     INTEGER NOT NULL,
    create_time TEXT    DEFAULT (datetime('now', 'localtime')),
    UNIQUE (post_id, user_id)
);

-- ------------------------------------------------------------
-- 12. 用户关注表 user_follow
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_follow (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    follower_id INTEGER NOT NULL,
    followee_id INTEGER NOT NULL,
    create_time TEXT    DEFAULT (datetime('now', 'localtime')),
    UNIQUE (follower_id, followee_id)
);

-- ------------------------------------------------------------
-- 13. 食谱收藏表 recipe_collect
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS recipe_collect (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER NOT NULL,
    recipe_id   INTEGER NOT NULL,
    category    TEXT    NOT NULL DEFAULT '默认',
    create_time TEXT    DEFAULT (datetime('now', 'localtime')),
    UNIQUE (user_id, recipe_id)
);

-- ------------------------------------------------------------
-- 14. 食谱评分反馈表 recipe_rating
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS recipe_rating (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    recipe_id   INTEGER NOT NULL,
    user_id     INTEGER NOT NULL,
    score       INTEGER NOT NULL,
    feedback    TEXT    DEFAULT '',
    deleted     INTEGER NOT NULL DEFAULT 0,
    create_time TEXT    DEFAULT (datetime('now', 'localtime')),
    UNIQUE (user_id, recipe_id)
);

-- ------------------------------------------------------------
-- 15. 内容举报表 content_report
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS content_report (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    reporter_id  INTEGER NOT NULL,
    target_type  TEXT    NOT NULL,
    target_id    INTEGER NOT NULL,
    reason       TEXT    NOT NULL,
    status       INTEGER NOT NULL DEFAULT 0,
    handle_result TEXT,
    create_time  TEXT    DEFAULT (datetime('now', 'localtime')),
    handle_time  TEXT
);
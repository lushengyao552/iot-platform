-- ============================================================
-- 图书管理系统 - 数据库初始化脚本
-- 数据库：MySQL 8.0+
-- 字符集：utf8mb4
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS library_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE library_db;

-- ============================================================
-- 1. 用户表
-- ============================================================
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
    nickname    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色：ADMIN-管理员，USER-普通用户',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_email (email),
    KEY idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 2. 图书分类表
-- ============================================================
DROP TABLE IF EXISTS book_category;
CREATE TABLE book_category (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    name        VARCHAR(50)  NOT NULL COMMENT '分类名称',
    description VARCHAR(255) DEFAULT NULL COMMENT '分类描述',
    sort        INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书分类表';

-- ============================================================
-- 3. 图书表
-- ============================================================
DROP TABLE IF EXISTS book;
CREATE TABLE book (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '图书ID',
    isbn          VARCHAR(20)   NOT NULL COMMENT 'ISBN编号',
    title         VARCHAR(200)  NOT NULL COMMENT '书名',
    author        VARCHAR(100)  NOT NULL COMMENT '作者',
    publisher     VARCHAR(100)  DEFAULT NULL COMMENT '出版社',
    publish_date  DATE          DEFAULT NULL COMMENT '出版日期',
    category_id   BIGINT        DEFAULT NULL COMMENT '分类ID',
    price         DECIMAL(10,2) DEFAULT NULL COMMENT '价格',
    stock         INT           NOT NULL DEFAULT 0 COMMENT '库存数量',
    total_stock   INT           NOT NULL DEFAULT 0 COMMENT '总藏书量',
    description   TEXT          DEFAULT NULL COMMENT '图书简介',
    cover_url     VARCHAR(500)  DEFAULT NULL COMMENT '封面图片URL',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_isbn (isbn),
    KEY idx_title (title),
    KEY idx_author (author),
    KEY idx_category_id (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书表';

-- ============================================================
-- 4. 借阅记录表
-- ============================================================
DROP TABLE IF EXISTS borrow_record;
CREATE TABLE borrow_record (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    user_id     BIGINT        NOT NULL COMMENT '用户ID',
    book_id     BIGINT        NOT NULL COMMENT '图书ID',
    borrow_date DATE          NOT NULL COMMENT '借阅日期',
    due_date    DATE          NOT NULL COMMENT '应还日期',
    return_date DATE          DEFAULT NULL COMMENT '实际归还日期',
    status      VARCHAR(20)   NOT NULL DEFAULT 'BORROWED' COMMENT '状态：BORROWED-借阅中，RETURNED-已归还，OVERDUE-已逾期',
    fine        DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '逾期罚款',
    create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_book_id (book_id),
    KEY idx_status (status),
    KEY idx_borrow_date (borrow_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='借阅记录表';

-- ============================================================
-- 初始数据
-- ============================================================

-- 管理员账号：admin / admin123（BCrypt加密）
-- 普通用户：user / user123
INSERT INTO sys_user (username, password, nickname, email, phone, role, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIu', '系统管理员', 'admin@example.com', '13800000001', 'ADMIN', 1),
('user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIu', '普通用户', 'user@example.com', '13800000002', 'USER', 1),
('zhangsan', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIu', '张三', 'zhangsan@example.com', '13800000003', 'USER', 1);

-- 图书分类
INSERT INTO book_category (name, description, sort) VALUES
('计算机科学', '计算机编程、算法、数据结构等技术书籍', 1),
('文学小说', '中外文学作品、小说、散文等', 2),
('历史人文', '历史、哲学、社会学等人文类书籍', 3),
('经济管理', '经济学、管理学、商业类书籍', 4),
('科学技术', '自然科学、工程技术类书籍', 5),
('艺术设计', '美术、设计、音乐等艺术类书籍', 6);

-- 图书数据
INSERT INTO book (isbn, title, author, publisher, publish_date, category_id, price, stock, total_stock, description) VALUES
('9787111213826', 'Java核心技术 卷I：基础知识', 'Cay S. Horstmann', '机械工业出版社', '2022-01-01', 1, 119.00, 5, 5, 'Java领域最有影响力和价值的著作之一，全面覆盖Java语言的核心概念。'),
('9787115428028', '深入理解Java虚拟机', '周志明', '人民邮电出版社', '2019-12-01', 1, 89.00, 3, 3, '从Java内存模型、垃圾回收、类加载机制等方面深入剖析JVM。'),
('9787121362149', 'Spring实战（第6版）', 'Craig Walls', '电子工业出版社', '2022-03-01', 1, 109.00, 4, 4, 'Spring框架权威指南，涵盖Spring Boot、Spring Security等核心技术。'),
('9787020002207', '红楼梦', '曹雪芹', '人民文学出版社', '1996-12-01', 2, 59.70, 10, 10, '中国古典四大名著之首，中国封建社会的百科全书。'),
('9787544253994', '百年孤独', '加西亚·马尔克斯', '南海出版公司', '2011-06-01', 2, 39.50, 6, 6, '魔幻现实主义文学的代表作，布恩迪亚家族七代人的传奇故事。'),
('9787108008374', '万历十五年', '黄仁宇', '生活·读书·新知三联书店', '1997-05-01', 3, 18.00, 8, 8, '以大历史观视角解读明朝万历年间的社会与政治。'),
('9787508660752', '人类简史', '尤瓦尔·赫拉利', '中信出版社', '2017-02-01', 3, 68.00, 5, 5, '从认知革命、农业革命到科学革命，讲述人类发展的宏大叙事。'),
('9787111407010', '算法导论（原书第3版）', 'Thomas H. Cormen', '机械工业出版社', '2013-01-01', 1, 128.00, 2, 2, '计算机算法领域的经典教材，全面涵盖算法设计与分析。'),
('9787115546081', '代码整洁之道', 'Robert C. Martin', '人民邮电出版社', '2020-06-01', 1, 69.00, 4, 4, '软件工程师必读，讲解如何编写可读性强、可维护的代码。'),
('9787521727302', '置身事内：中国政府与经济发展', '兰小欢', '上海人民出版社', '2021-08-01', 4, 65.00, 3, 3, '从地方政府的视角理解中国经济发展的逻辑。');

-- 借阅记录示例
INSERT INTO borrow_record (user_id, book_id, borrow_date, due_date, return_date, status, fine) VALUES
(2, 1, '2026-08-01', '2026-08-31', NULL, 'BORROWED', 0.00),
(2, 4, '2026-07-15', '2026-08-14', '2026-08-10', 'RETURNED', 0.00),
(3, 2, '2026-07-01', '2026-07-31', NULL, 'OVERDUE', 15.00);

-- ========================================
-- 电子商品推荐系统数据库设计
-- ========================================

CREATE DATABASE IF NOT EXISTS recommendation_system
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

USE recommendation_system;

-- ========================================
-- 1. 用户表 (users)
-- ========================================
DROP TABLE IF EXISTS users;
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(加密)',
    email VARCHAR(100) UNIQUE COMMENT '邮箱',
    phone VARCHAR(20) UNIQUE COMMENT '手机号',
    nickname VARCHAR(50) COMMENT '昵称',
    avatar VARCHAR(255) COMMENT '头像URL',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-正常, 0-禁用',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ========================================
-- 2. 商品表 (products)
-- ========================================
DROP TABLE IF EXISTS products;
CREATE TABLE products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品ID',
    name VARCHAR(200) NOT NULL COMMENT '商品名称',
    description TEXT COMMENT '商品描述',
    category_id BIGINT COMMENT '分类ID',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    stock INT DEFAULT 0 COMMENT '库存数量',
    sales INT DEFAULT 0 COMMENT '销量',
    image_url VARCHAR(500) COMMENT '主图URL',
    images JSON COMMENT '商品图片列表',
    semantic_tags JSON COMMENT '语义标签: {"style":"复古","faceShape":"圆脸"}',
    ai_summary TEXT COMMENT 'AI生成的商品摘要',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-上架, 0-下架',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category (category_id),
    INDEX idx_price (price),
    INDEX idx_sales (sales),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- ========================================
-- 3. 商品分类表 (categories)
-- ========================================
DROP TABLE IF EXISTS categories;
CREATE TABLE categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID',
    level INT DEFAULT 1 COMMENT '分类层级',
    sort_order INT DEFAULT 0 COMMENT '排序',
    icon VARCHAR(255) COMMENT '图标URL',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent (parent_id),
    INDEX idx_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- ========================================
-- 4. 用户评分表 (ratings)
-- ========================================
DROP TABLE IF EXISTS ratings;
CREATE TABLE ratings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评分ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    score DECIMAL(2,1) NOT NULL COMMENT '评分(1.0-5.0)',
    comment TEXT COMMENT '评论内容',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_product (user_id, product_id),
    INDEX idx_product (product_id),
    INDEX idx_score (score),
    CONSTRAINT fk_rating_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_rating_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户评分表';

-- ========================================
-- 5. 用户行为表 (user_behavior)
-- ========================================
DROP TABLE IF EXISTS user_behavior;
CREATE TABLE user_behavior (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '行为ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    behavior_type TINYINT NOT NULL COMMENT '行为类型: 1-浏览, 2-收藏, 3-加购, 4-购买',
    duration INT DEFAULT 0 COMMENT '停留时长(秒)',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user (user_id),
    INDEX idx_product (product_id),
    INDEX idx_type (behavior_type),
    INDEX idx_time (created_time),
    CONSTRAINT fk_behavior_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_behavior_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为表';

-- ========================================
-- 6. 订单表 (orders)
-- ========================================
DROP TABLE IF EXISTS orders;
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    order_no VARCHAR(32) UNIQUE NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-待支付, 1-已支付, 2-已发货, 3-已完成, 4-已取消',
    receiver_name VARCHAR(50) COMMENT '收货人姓名',
    receiver_phone VARCHAR(20) COMMENT '收货人电话',
    receiver_address TEXT COMMENT '收货地址',
    remark TEXT COMMENT '备注',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user (user_id),
    INDEX idx_order_no (order_no),
    INDEX idx_status (status),
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ========================================
-- 7. 订单详情表 (order_items)
-- ========================================
DROP TABLE IF EXISTS order_items;
CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '详情ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    product_name VARCHAR(200) NOT NULL COMMENT '商品名称',
    product_image VARCHAR(500) COMMENT '商品图片',
    price DECIMAL(10,2) NOT NULL COMMENT '商品单价',
    quantity INT NOT NULL COMMENT '购买数量',
    total_price DECIMAL(10,2) NOT NULL COMMENT '小计金额',
    INDEX idx_order (order_id),
    INDEX idx_product (product_id),
    CONSTRAINT fk_order_detail FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单详情表';

-- ========================================
-- 8. 购物车表 (cart_items)
-- ========================================
DROP TABLE IF EXISTS cart_items;
CREATE TABLE cart_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '购物车ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    quantity INT NOT NULL DEFAULT 1 COMMENT '商品数量',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_product (user_id, product_id),
    INDEX idx_user (user_id),
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- ========================================
-- 9. 推荐结果表 (recommendations)
-- ========================================
DROP TABLE IF EXISTS recommendations;
CREATE TABLE recommendations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '推荐ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    score DECIMAL(10,6) NOT NULL COMMENT '推荐得分',
    reason VARCHAR(100) COMMENT '推荐理由',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user (user_id),
    INDEX idx_score (score),
    INDEX idx_time (created_time),
    CONSTRAINT fk_rec_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_rec_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐结果表';

-- ========================================
-- 初始化数据
-- ========================================

-- 插入管理员用户 (密码: admin123)
INSERT INTO users (username, password, email, phone, nickname, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@example.com', '13800138000', '超级管理员', 1);

-- 插入测试用户 (密码: user123)
INSERT INTO users (username, password, email, phone, nickname, status) VALUES
('user1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'user1@example.com', '13800138001', '测试用户1', 1),
('user2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'user2@example.com', '13800138002', '测试用户2', 1);

-- 插入商品分类
INSERT INTO categories (name, parent_id, level, sort_order) VALUES
('数码产品', 0, 1, 1),
('服装鞋帽', 0, 1, 2),
('食品饮料', 0, 1, 3),
('家居用品', 0, 1, 4),
('图书文具', 0, 1, 5);

INSERT INTO categories (name, parent_id, level, sort_order) VALUES
('手机', 1, 2, 1),
('电脑', 1, 2, 2),
('平板', 1, 2, 3),
('男装', 2, 2, 1),
('女装', 2, 2, 2),
('运动鞋', 2, 2, 3);

-- 插入商品
INSERT INTO products (name, description, category_id, price, stock, sales, image_url, status) VALUES
('iPhone 15 Pro Max', '苹果旗舰手机,钛金属机身,A17 Pro芯片', 6, 9999.00, 100, 500, 'https://example.com/images/iphone15.jpg', 1),
('MacBook Pro 14英寸', 'M3 Pro芯片,14英寸Liquid Retina XDR显示屏', 7, 16999.00, 50, 200, 'https://example.com/images/macbook.jpg', 1),
('iPad Air 5', 'M1芯片,10.9英寸Liquid视网膜显示屏', 8, 4799.00, 80, 300, 'https://example.com/images/ipad.jpg', 1),
('男士休闲西装', '精选面料,修身剪裁,商务休闲两相宜', 9, 599.00, 200, 150, 'https://example.com/images/suit.jpg', 1),
('女士连衣裙', '韩版设计,透气舒适,时尚百搭', 10, 299.00, 150, 250, 'https://example.com/images/dress.jpg', 1),
('Nike运动鞋', 'Air Max气垫,透气网面,缓震舒适', 11, 799.00, 120, 400, 'https://example.com/images/nike.jpg', 1);

-- 插入用户评分
INSERT INTO ratings (user_id, product_id, score, comment) VALUES
(2, 1, 4.5, '手机很好用,系统流畅'),
(2, 2, 5.0, '性能强劲,屏幕超棒'),
(2, 3, 4.0, '性价比很高'),
(2, 4, 3.5, '质量还行,尺码偏小'),
(3, 1, 5.0, '超级喜欢!'),
(3, 5, 4.0, '裙子很漂亮'),
(3, 6, 3.0, '鞋子一般般'),
(3, 2, 4.5, '工作利器');

-- 插入用户行为
INSERT INTO user_behavior (user_id, product_id, behavior_type, duration) VALUES
(2, 1, 1, 180),
(2, 2, 1, 300),
(2, 3, 2, 0),
(2, 4, 3, 0),
(3, 1, 1, 240),
(3, 5, 1, 120),
(3, 6, 1, 90),
(3, 2, 1, 360);

-- ========================================
-- 10. 对话会话表 (chat_sessions)
-- ========================================
DROP TABLE IF EXISTS chat_sessions;
CREATE TABLE chat_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(200) DEFAULT '新的对话' COMMENT '会话标题',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-活跃, 0-已归档',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user (user_id),
    INDEX idx_time (created_time),
    CONSTRAINT fk_chat_session_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话会话表';

-- ========================================
-- 11. 对话消息表 (chat_messages)
-- ========================================
DROP TABLE IF EXISTS chat_messages;
CREATE TABLE chat_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    session_id BIGINT NOT NULL COMMENT '会话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色: user|assistant|system|function',
    content TEXT COMMENT '消息内容',
    function_name VARCHAR(100) COMMENT 'Function Call函数名',
    metadata JSON COMMENT '扩展元数据(产品卡片等)',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_session (session_id),
    INDEX idx_time (created_time),
    CONSTRAINT fk_msg_session FOREIGN KEY (session_id) REFERENCES chat_sessions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话消息表';

-- ========================================
-- 商品表扩展: 添加语义标签字段
-- ========================================
-- ========================================
-- 数据库升级脚本(已有数据库执行, 新库无需执行)
-- ========================================
-- ALTER TABLE products ADD COLUMN semantic_tags JSON COMMENT '语义标签';
-- ALTER TABLE products ADD COLUMN ai_summary TEXT COMMENT 'AI生成摘要';

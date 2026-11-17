-- ========================================
-- 商品分类与商品数据插入脚本（幂等，可重复执行）
-- 覆盖：电子数码、服装鞋帽、食品饮料、家居用品、图书文具、
--       运动户外、美妆护肤、母婴玩具、汽车配件、宠物用品
-- 说明：本脚本清空业务数据后重建种子数据；首次使用请先执行 schema.sql 建表
-- ========================================

USE recommendation_system;

-- ========================================
-- 清空业务数据（按外键依赖顺序，先子表后父表）
-- ========================================
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM order_items;
DELETE FROM orders;
DELETE FROM cart_items;
DELETE FROM recommendations;
DELETE FROM user_behavior;
DELETE FROM ratings;
DELETE FROM chat_messages;
DELETE FROM chat_sessions;
DELETE FROM products;
DELETE FROM categories;
DELETE FROM users;
SET FOREIGN_KEY_CHECKS = 1;

-- 重置自增，保证重复执行后主键稳定
ALTER TABLE users AUTO_INCREMENT = 1;
ALTER TABLE categories AUTO_INCREMENT = 1;
ALTER TABLE products AUTO_INCREMENT = 1;
ALTER TABLE ratings AUTO_INCREMENT = 1;

-- ========================================
-- 用户（密码：admin123 / user123，BCrypt 加密）
-- ========================================
INSERT INTO users (id, username, password, email, phone, nickname, status) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'admin@example.com', '13800138000', '超级管理员', 1),
(2, 'user1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'user1@example.com', '13800138001', '测试用户1', 1),
(3, 'user2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'user2@example.com', '13800138002', '测试用户2', 1);

-- ========================================
-- 一级分类（10大类）
-- ========================================
INSERT INTO categories (id, name, parent_id, level, sort_order) VALUES
(1,  '电子数码', 0, 1, 1),
(2,  '服装鞋帽', 0, 1, 2),
(3,  '食品饮料', 0, 1, 3),
(4,  '家居用品', 0, 1, 4),
(5,  '图书文具', 0, 1, 5),
(6,  '运动户外', 0, 1, 6),
(7,  '美妆护肤', 0, 1, 7),
(8,  '母婴玩具', 0, 1, 8),
(9,  '汽车配件', 0, 1, 9),
(10, '宠物用品', 0, 1, 10);

-- ========================================
-- 二级分类
-- ========================================
INSERT INTO categories (id, name, parent_id, level, sort_order) VALUES
-- 电子数码子类
(11, '手机通讯',   1, 2, 1),
(12, '电脑办公',   1, 2, 2),
(13, '平板电脑',   1, 2, 3),
(14, '智能穿戴',   1, 2, 4),
(15, '影音娱乐',   1, 2, 5),
(16, '摄影摄像',   1, 2, 6),
(17, '电子配件',   1, 2, 7),
-- 服装鞋帽子类
(18, '男装',       2, 2, 1),
(19, '女装',       2, 2, 2),
(20, '运动鞋服',   2, 2, 3),
(21, '内衣睡衣',   2, 2, 4),
(22, '箱包皮具',   2, 2, 5),
-- 食品饮料子类
(23, '零食坚果',   3, 2, 1),
(24, '酒水饮料',   3, 2, 2),
(25, '粮油调味',   3, 2, 3),
(26, '生鲜食品',   3, 2, 4),
(27, '茶叶咖啡',   3, 2, 5),
-- 家居用品子类
(28, '床上用品',   4, 2, 1),
(29, '厨房用具',   4, 2, 2),
(30, '清洁卫浴',   4, 2, 3),
(31, '家居装饰',   4, 2, 4),
(32, '照明电器',   4, 2, 5),
-- 图书文具子类
(33, '小说文学',   5, 2, 1),
(34, '教育学习',   5, 2, 2),
(35, '办公文具',   5, 2, 3),
(36, '艺术设计',   5, 2, 4),
-- 运动户外子类
(37, '健身器材',   6, 2, 1),
(38, '户外装备',   6, 2, 2),
(39, '球类运动',   6, 2, 3),
(40, '游泳装备',   6, 2, 4),
-- 美妆护肤子类
(41, '护肤品',     7, 2, 1),
(42, '彩妆',       7, 2, 2),
(43, '香水',       7, 2, 3),
(44, '美发护发',   7, 2, 4),
-- 母婴玩具子类
(45, '婴幼儿用品', 8, 2, 1),
(46, '儿童玩具',   8, 2, 2),
(47, '童装童鞋',   8, 2, 3),
-- 汽车配件子类
(48, '汽车电子',   9, 2, 1),
(49, '汽车内饰',   9, 2, 2),
(50, '汽车养护',   9, 2, 3),
-- 宠物用品子类
(51, '猫咪用品',   10, 2, 1),
(52, '狗狗用品',   10, 2, 2),
(53, '宠物食品',   10, 2, 3);

-- ========================================
-- 商品数据
-- ========================================

INSERT INTO products (name, description, category_id, price, stock, sales, image_url, status) VALUES

-- ===== 手机通讯 (category_id=11) =====
('iPhone 15 Pro Max 256G 黑色钛', '苹果旗舰手机，A17 Pro芯片，钛金属机身，4800万主摄，支持USB-C', 11, 9999.00, 200, 1520, 'https://example.com/products/iphone15promax.jpg', 1),
('iPhone 15 128G 粉色', '苹果标准款，A16芯片，灵动岛设计，全天候续航', 11, 5999.00, 350, 2100, 'https://example.com/products/iphone15.jpg', 1),
('华为 Mate 60 Pro 512G', '麒麟9000S旗舰芯片，卫星通话功能，超长续航，徕卡影像', 11, 6999.00, 180, 980, 'https://example.com/products/mate60pro.jpg', 1),
('华为 nova 12 256G 樱粉色', '时尚轻薄机身，前置6000万人像相机，快充支持', 11, 2699.00, 400, 1230, 'https://example.com/products/nova12.jpg', 1),
('小米 14 Pro 512G', '骁龙8 Gen3旗舰，徕卡专业影像，第二代超声波指纹', 11, 5299.00, 260, 1450, 'https://example.com/products/mi14pro.jpg', 1),
('小米 Redmi Note 13 Pro 256G', '2亿像素主摄，1.5K OLED曲面屏，67W快充', 11, 1899.00, 600, 3200, 'https://example.com/products/redminote13pro.jpg', 1),
('OPPO Find X7 Ultra 512G', '哈苏四摄，骁龙8 Gen3，100W超级快充，潜望式长焦', 11, 6999.00, 150, 760, 'https://example.com/products/findx7ultra.jpg', 1),
('vivo X100 Pro 256G 白月光', '蔡司影像，天玑9300旗舰，蓝海电池超长续航', 11, 4999.00, 220, 890, 'https://example.com/products/vivox100pro.jpg', 1),
('三星 Galaxy S24 Ultra 256G', '钛金属边框，S Pen手写笔，2亿像素摄像头，AI功能', 11, 9499.00, 120, 540, 'https://example.com/products/s24ultra.jpg', 1),
('荣耀 Magic6 Pro 256G', '第三代骁龙8，鹰眼相机，5800mAh大电池', 11, 4499.00, 280, 670, 'https://example.com/products/magic6pro.jpg', 1),

-- ===== 电脑办公 (category_id=12) =====
('MacBook Pro 14英寸 M3 Pro', 'M3 Pro芯片，16GB内存，512GB存储，Liquid Retina XDR显示屏', 12, 16999.00, 80, 320, 'https://example.com/products/macbookpro14.jpg', 1),
('MacBook Air 13英寸 M2', 'M2芯片，8GB内存，256GB存储，轻薄设计，全天候续航', 12, 9499.00, 150, 680, 'https://example.com/products/macbookair13.jpg', 1),
('联想 ThinkPad X1 Carbon', '英特尔Core i7，16GB内存，1TB SSD，商务本首选', 12, 11999.00, 60, 210, 'https://example.com/products/thinkpadx1.jpg', 1),
('华为 MateBook 16s', '英特尔酷睿i9-13900H，16GB内存，1TB SSD，2.5K高色域屏', 12, 8999.00, 100, 380, 'https://example.com/products/matebook16s.jpg', 1),
('小米笔记本Pro 14', '英特尔Ultra 7，32GB内存，2.8K OLED屏，RTX 4060独显', 12, 7999.00, 120, 450, 'https://example.com/products/minotebook14.jpg', 1),
('戴尔 XPS 15 4K OLED', '英特尔i7-13700H，32GB，1TB SSD，GTX 4060，4K触控屏', 12, 14999.00, 40, 150, 'https://example.com/products/dellxps15.jpg', 1),
('惠普 HP OMEN 16 游戏本', 'AMD Ryzen 7，RTX 4070显卡，165Hz高刷屏，电竞性能', 12, 9999.00, 90, 280, 'https://example.com/products/hpomen16.jpg', 1),
('罗技 MX Master 3S 无线鼠标', '8000DPI追踪，MagSpeed电磁滚轮，静音按键，跨设备操控', 12, 899.00, 500, 2400, 'https://example.com/products/mxmaster3s.jpg', 1),
('机械革命 极光Z 机械键盘', '红轴/茶轴可选，RGB背光，铝合金面板，99键布局', 12, 499.00, 300, 1800, 'https://example.com/products/keyboard_jiguang.jpg', 1),
('AOC 27英寸 4K显示器', '4K IPS面板，HDR400，10.7亿色，99%sRGB，旋转升降底座', 12, 2199.00, 160, 630, 'https://example.com/products/aoc27.jpg', 1),

-- ===== 平板电脑 (category_id=13) =====
('iPad Pro 12.9英寸 M2 512G', 'M2芯片，Liquid Retina XDR，支持Apple Pencil和妙控键盘', 13, 9999.00, 90, 340, 'https://example.com/products/ipadpro12.jpg', 1),
('iPad Air 10.9英寸 M1 256G', 'M1芯片，10.9英寸全面屏，Touch ID，支持Apple Pencil第二代', 13, 5799.00, 130, 560, 'https://example.com/products/ipadair.jpg', 1),
('华为 MatePad Pro 11英寸', '麒麟9000S，OLED护眼屏，鸿蒙OS，支持华为手写笔', 13, 4999.00, 100, 430, 'https://example.com/products/matepadpro11.jpg', 1),
('小米平板6 Pro 12.4英寸', '骁龙8+ Gen1，2.8K 144Hz屏，10000mAh电池，67W快充', 13, 3299.00, 180, 720, 'https://example.com/products/mipadzero6pro.jpg', 1),
('三星 Galaxy Tab S9+ 256G', '骁龙8 Gen2，12.4英寸Dynamic AMOLED，防水防尘，配S Pen', 13, 7499.00, 70, 220, 'https://example.com/products/tabs9plus.jpg', 1),

-- ===== 智能穿戴 (category_id=14) =====
('Apple Watch Series 9 铝金属', '血氧、心率、ECG监测，精准运动追踪，WatchOS 10', 14, 3299.00, 200, 890, 'https://example.com/products/applewatch9.jpg', 1),
('华为 Watch GT4 46mm', '双频GPS，14天续航，精准健康监测，多运动模式', 14, 1698.00, 280, 1350, 'https://example.com/products/watchgt4.jpg', 1),
('小米手环8 Pro', '1.74英寸大屏，150+运动模式，血氧心率监测，NFC门禁', 14, 399.00, 800, 5600, 'https://example.com/products/miband8pro.jpg', 1),
('三星 Galaxy Watch 6 Classic', '旋转表圈，BioActive传感器，运动追踪，精准睡眠监测', 14, 2899.00, 110, 430, 'https://example.com/products/galaxywatch6classic.jpg', 1),
('AirPods Pro 第二代', '主动降噪，Transparency模式，H2芯片，IP54防水', 14, 1899.00, 350, 2100, 'https://example.com/products/airpodspro2.jpg', 1),
('索尼 WF-1000XM5 无线耳机', '行业领先降噪，30小时续航，精准语音通话，LDAC高音质', 14, 2199.00, 180, 760, 'https://example.com/products/sonywf1000xm5.jpg', 1),
('华为 FreeBuds Pro 3', '三驱三单元设计，双芯交融，业界领先降噪', 14, 1399.00, 220, 980, 'https://example.com/products/freebuspro3.jpg', 1),

-- ===== 影音娱乐 (category_id=15) =====
('索尼 PlayStation 5 光驱版', '次时代主机，4K/120fps游戏，DualSense手柄，超高速SSD', 15, 3799.00, 80, 420, 'https://example.com/products/ps5.jpg', 1),
('任天堂 Switch OLED 版', '7英寸OLED屏，64GB存储，TV/桌面/掌机三模切换', 15, 2299.00, 150, 1100, 'https://example.com/products/switch_oled.jpg', 1),
('小米电视 Redmi X65 4K', '65英寸，4K HDR，144Hz高刷，120Hz运动补偿，2+32G', 15, 3499.00, 60, 230, 'https://example.com/products/redmitv65.jpg', 1),
('海信 ULED X 75英寸 Mini LED', '75英寸，2000nit峰值亮度，IMAX影院认证，144Hz高刷', 15, 9999.00, 30, 90, 'https://example.com/products/hisenseuledx75.jpg', 1),
('索尼 HT-A7000 回音壁', '7.1.2声道，360°空间音效，HDMI 2.1，内置双低音单元', 15, 8999.00, 20, 55, 'https://example.com/products/hta7000.jpg', 1),

-- ===== 摄影摄像 (category_id=16) =====
('索尼 A7M4 全画幅微单套机', '3300万像素，15级动态范围，眼部AF追踪，4K 60fps视频', 16, 19999.00, 30, 120, 'https://example.com/products/sonya7m4.jpg', 1),
('佳能 EOS R6 Mark II', '2420万像素，6K RAW视频，深度学习AF，10fps连拍', 16, 18999.00, 25, 95, 'https://example.com/products/canoneosr6ii.jpg', 1),
('大疆 DJI Mini 4 Pro 无人机', '4K/60fps全向避障，51分钟超长续航，竖拍模式，10km图传', 16, 4799.00, 100, 560, 'https://example.com/products/djimini4pro.jpg', 1),
('GoPro HERO12 Black 运动相机', '5.3K视频，防水10米，超广角镜头，HyperSmooth 6.0防抖', 16, 2999.00, 150, 780, 'https://example.com/products/gopro12.jpg', 1),
('富士 Instax mini 12 拍立得', '自动曝光，自拍镜，多种胶片效果，时尚外观', 16, 599.00, 400, 2300, 'https://example.com/products/instaxmini12.jpg', 1),

-- ===== 电子配件 (category_id=17) =====
('苹果 MagSafe 充电器 15W', '兼容iPhone 12及以上，磁吸精准对位，最高15W无线快充', 17, 329.00, 600, 4500, 'https://example.com/products/magsafe.jpg', 1),
('Anker 140W 氮化镓充电器', '三口充电，GaN技术，支持PD3.1，兼容笔记本手机平板', 17, 259.00, 800, 6200, 'https://example.com/products/anker140w.jpg', 1),
('绿联 USB-C Hub 9合1', '4K HDMI，100W PD，SD卡槽，千兆网口，USB3.0*3', 17, 199.00, 500, 3800, 'https://example.com/products/ugreen9in1.jpg', 1),
('西部数据 My Passport 2TB 移动硬盘', 'USB-C接口，自动备份，密码保护，轻薄便携', 17, 479.00, 300, 1600, 'https://example.com/products/wdmypassport2tb.jpg', 1),
('三星 T7 Shield 2TB 移动固态硬盘', ' IP65防尘防水，1050MB/s读速，军规抗摔，3年保修', 17, 799.00, 200, 950, 'https://example.com/products/t7shield2tb.jpg', 1),

-- ===== 男装 (category_id=18) =====
('优衣库 男士摇粒绒拉链外套', '超柔软面料，保暖舒适，休闲百搭，多色可选', 18, 299.00, 500, 3200, 'https://example.com/products/uc_fleece.jpg', 1),
('海澜之家 商务休闲西装套装', '修身版型，精纺面料，商务会议首选，抗皱好打理', 18, 899.00, 200, 780, 'https://example.com/products/heilan_suit.jpg', 1),
('耐克 男子运动卫衣', 'Dri-FIT排汗面料，宽松舒适，适合健身跑步，多色选', 18, 399.00, 400, 1850, 'https://example.com/products/nike_hoodie.jpg', 1),
('Levi 501 经典直筒牛仔裤', '经典款式，纯棉面料，耐磨耐穿，百搭休闲', 18, 599.00, 300, 1430, 'https://example.com/products/levis501.jpg', 1),
('波司登 男士羽绒服 800蓬', '90%白鸭绒，防风防水，智能恒温，经典立领设计', 18, 1299.00, 150, 560, 'https://example.com/products/bosideng_down.jpg', 1),

-- ===== 女装 (category_id=19) =====
('ZARA 女士印花连衣裙', '波西米亚风，轻薄面料，腰部收腰设计，优雅知性', 19, 359.00, 350, 2100, 'https://example.com/products/zara_dress.jpg', 1),
('Only 女士修身小西装', '精纺毛料，OL风格，内搭任意百搭，通勤首选', 19, 499.00, 280, 1340, 'https://example.com/products/only_blazer.jpg', 1),
('太平鸟 女士针织毛衣', '羊绒混纺，柔软亲肤，宽松慵懒风，秋冬必备', 19, 399.00, 320, 1560, 'https://example.com/products/peacebird_sweater.jpg', 1),
('迪奥 女士蕾丝上衣', '高品质蕾丝，精致刺绣，修身剪裁，宴会首选', 19, 3599.00, 50, 180, 'https://example.com/products/dior_top.jpg', 1),
('HM 女士高腰阔腿裤', '棉麻混纺，宽松舒适，多口袋设计，休闲通勤两用', 19, 199.00, 600, 4200, 'https://example.com/products/hm_trousers.jpg', 1),

-- ===== 运动鞋服 (category_id=20) =====
('Nike Air Max 270 运动跑鞋', 'Air Max气垫，透气网面鞋身，时尚街头风，男女可穿', 20, 899.00, 250, 1890, 'https://example.com/products/airmax270.jpg', 1),
('Adidas Ultra Boost 22 跑鞋', 'Boost缓震中底，Primeknit针织鞋面，马拉松级回弹', 20, 1099.00, 200, 1320, 'https://example.com/products/ultraboost22.jpg', 1),
('New Balance 574 复古休闲鞋', '经典款式，麂皮面料，ENCAP中底科技，舒适耐穿', 20, 649.00, 300, 2100, 'https://example.com/products/nb574.jpg', 1),
('李宁 篮球鞋 空袭 7', '李宁弓科技，专业篮球支撑，防滑橡胶大底，国潮设计', 20, 799.00, 180, 890, 'https://example.com/products/lining_bball.jpg', 1),
('安踏 男士运动套装', '速干面料，弹力设计，多口袋，跑步健身两相宜', 20, 299.00, 400, 2600, 'https://example.com/products/anta_sportsuit.jpg', 1),

-- ===== 箱包皮具 (category_id=22) =====
('新秀丽 25寸旅行箱 铝框', '飞机轮，TSA密码锁，轻量铝框，静音滑轮', 22, 1599.00, 150, 760, 'https://example.com/products/samsonite_25.jpg', 1),
('Coach 女士小号手提包', '精选牛皮，多格收纳，磁扣开合，百搭通勤包', 22, 2299.00, 80, 340, 'https://example.com/products/coach_bag.jpg', 1),
('大容量双肩包 男士旅行背包', '防水面料，USB充电口，多层收纳，人体工学背带', 22, 269.00, 500, 3400, 'https://example.com/products/travel_backpack.jpg', 1),
('无印良品 帆布托特包', '棉帆布材质，简约实用，多色可选，购物通勤两用', 22, 189.00, 600, 4800, 'https://example.com/products/muji_tote.jpg', 1),

-- ===== 零食坚果 (category_id=23) =====
('三只松鼠 混合坚果礼盒 1kg', '核桃、腰果、夏威夷果等8种坚果，无添加蔗糖，健康美味', 23, 89.90, 1000, 8500, 'https://example.com/products/squirrel_nuts.jpg', 1),
('洽洽 恰恰瓜子 500g', '香瓜子，精选籽粒，独特炒制工艺，回味悠长', 23, 19.90, 2000, 15000, 'https://example.com/products/qiaqia_seeds.jpg', 1),
('乐事 薯片多口味礼包 20包', '原味/番茄/牛排等多口味，薄脆口感，聚会零食首选', 23, 39.90, 1500, 12000, 'https://example.com/products/lays_multipack.jpg', 1),
('百草味 蔓越莓曲奇 300g', '黄油曲奇，颗粒满满，酥脆香甜，送礼自吃两相宜', 23, 35.90, 800, 5600, 'https://example.com/products/bcw_cookie.jpg', 1),
('奥利奥 夹心饼干 434g', '经典款式，黑白饼干，奶油夹心，多种吃法', 23, 22.90, 1800, 20000, 'https://example.com/products/oreo.jpg', 1),
('费列罗 榛果威化巧克力 16粒', '意大利进口，榛子+威化+巧克力，金色礼盒，节日送礼', 23, 69.90, 600, 4200, 'https://example.com/products/ferrero16.jpg', 1),

-- ===== 酒水饮料 (category_id=24) =====
('农夫山泉 矿泉水 550ml*24瓶', '天然弱碱性水，多种矿物质，日常饮用首选', 24, 29.90, 3000, 35000, 'https://example.com/products/nongfu_water24.jpg', 1),
('元气森林 气泡水 480ml*15瓶', '0糖0脂0卡，天然果味，多种口味可选，健康轻负担', 24, 49.90, 1500, 18000, 'https://example.com/products/yuanqi_sparkling.jpg', 1),
('红牛 功能饮料 250ml*24罐', '牛磺酸+咖啡因，提神醒脑，运动健身加油站', 24, 89.00, 800, 9500, 'https://example.com/products/redbull24.jpg', 1),
('茅台飞天 53度白酒 500ml', '酱香型，12987工艺，贵州茅台镇酿造，国宴用酒', 24, 1499.00, 50, 180, 'https://example.com/products/maotai.jpg', 1),
('张裕 解百纳 干红葡萄酒 750ml', '赤霞珠品种，橡木桶熟成，单宁细腻，适合佐餐', 24, 89.00, 400, 2300, 'https://example.com/products/changyu_wine.jpg', 1),
('青岛啤酒 经典 330ml*24听', '经典麦香，12°P，精酿工艺，清爽适口', 24, 79.00, 1200, 14000, 'https://example.com/products/qingdao_beer24.jpg', 1),

-- ===== 粮油调味 (category_id=25) =====
('金龙鱼 一级大豆油 5L', '物理精炼，富含维生素E，日常烹饪首选', 25, 69.90, 800, 6500, 'https://example.com/products/jlyu_5l.jpg', 1),
('五得利 麦芯粉 5kg', '精选优质小麦，超细研磨，口感细腻，包子馒头面条均可', 25, 35.90, 600, 4200, 'https://example.com/products/wudeli_flour.jpg', 1),
('李锦记 生抽酱油 500ml*3瓶', '头道鲜酱油，自然酿造180天，色泽红润，鲜香浓郁', 25, 39.90, 1000, 8800, 'https://example.com/products/lkk_soy3.jpg', 1),
('东北大米 五常稻花香 5kg', '黑土地种植，稻花香2号，米饭清香软糯，产地溯源', 25, 79.90, 700, 5600, 'https://example.com/products/wuchang_rice.jpg', 1),

-- ===== 茶叶咖啡 (category_id=27) =====
('大益 普洱茶 7572 357g熟茶', '经典熟茶配方，醇厚汤感，回甘持久，陈化潜力佳', 27, 199.00, 300, 1200, 'https://example.com/products/dayi7572.jpg', 1),
('西湖龙井 明前特级 100g', '浙江产区，明前采摘，扁平挺直，清香持久，茶味鲜爽', 27, 299.00, 200, 680, 'https://example.com/products/longjing.jpg', 1),
('雀巢 速溶咖啡三合一 100条', '咖啡+奶精+糖，方便快捷，办公室必备，醇香浓郁', 27, 89.90, 800, 7500, 'https://example.com/products/nescafe3in1.jpg', 1),
('星巴克 中度烘焙咖啡豆 250g', '中美洲产区，坚果香甜，均衡口感，适合手冲/意式', 27, 89.00, 400, 2300, 'https://example.com/products/starbucks_bean.jpg', 1),
('铁观音 特级乌龙茶 250g', '安溪产区，传统制法，兰花香气，七泡有余香', 27, 158.00, 250, 920, 'https://example.com/products/tieguanyin.jpg', 1),

-- ===== 床上用品 (category_id=28) =====
('水星家纺 四件套 纯棉', '60支长绒棉，柔软亲肤，多种花色，1.5/1.8米床通用', 28, 399.00, 400, 2800, 'https://example.com/products/mercury_bedding.jpg', 1),
('南极人 鹅绒被 冬被 200*230cm', '95%白鹅绒，蓬松保暖，轻薄舒适，防钻绒设计', 28, 599.00, 200, 980, 'https://example.com/products/nanjiren_duvet.jpg', 1),
('慕思 乳胶枕头 泰国进口', '天然乳胶，透气防螨，颈椎支撑，整夜舒适', 28, 499.00, 300, 1560, 'https://example.com/products/deroyal_pillow.jpg', 1),
('富安娜 加厚冬季毛毯 150*200cm', '双面绒设计，柔软保暖，午睡盖毯，沙发装饰两用', 28, 199.00, 500, 3200, 'https://example.com/products/fuanna_blanket.jpg', 1),

-- ===== 厨房用具 (category_id=29) =====
('苏泊尔 IH电磁炉 2100W', '智能温控，8档火力，超薄面板，多档安全保护', 29, 299.00, 300, 1800, 'https://example.com/products/supor_ih.jpg', 1),
('九阳 破壁料理机 1200W', '8叶钛合金刀头，豆浆/榨汁/研磨多功能，自动清洗', 29, 499.00, 250, 1350, 'https://example.com/products/joyoung_blender.jpg', 1),
('美的 电饭煲 5L 智能预约', 'IH电磁加热，24小时预约，多菜单模式，不粘内胆', 29, 599.00, 200, 1100, 'https://example.com/products/midea_ricecooker.jpg', 1),
('双立人 厨师刀 8英寸', '德国X50CrMoV15钢，人体工学手柄，锋利耐用，厨房必备', 29, 399.00, 180, 760, 'https://example.com/products/zwilling_knife.jpg', 1),
('乐扣乐扣 玻璃保鲜盒套装 5件', '耐热玻璃，四扣密封，可微波/烤箱，冰箱收纳首选', 29, 169.00, 600, 4200, 'https://example.com/products/locklock_glass5.jpg', 1),

-- ===== 清洁卫浴 (category_id=30) =====
('戴森 V15 Detect 无线吸尘器', '230AW强劲吸力，激光显尘技术，LCD计数，60分钟续航', 30, 4999.00, 80, 320, 'https://example.com/products/dysonv15.jpg', 1),
('科沃斯 扫拖一体机器人', '自动集尘，激光导航，APP控制，地毯自动加压', 30, 3299.00, 60, 240, 'https://example.com/products/ecovacs_robot.jpg', 1),
('立白 洗衣液 深层洁净 3kg', '天然酵素，深层去污，护色护纤维，低泡易漂', 30, 39.90, 1000, 9500, 'https://example.com/products/libai_laundry.jpg', 1),
('碧浪 洗衣凝珠 30颗', '3合1配方，深层洁净，去污+柔顺+去味，一颗搞定', 30, 49.90, 800, 7200, 'https://example.com/products/ariel_pods30.jpg', 1),
('松下 电动牙刷 W3', '声波震动，5档模式，30天续航，IPX7防水', 30, 299.00, 400, 2100, 'https://example.com/products/panasonic_toothbrush.jpg', 1),

-- ===== 家居装饰 (category_id=31) =====
('宜家 KALLAX 书架 4格', '简约北欧风，可配收纳盒，灵活组合，展示收纳两用', 31, 399.00, 150, 680, 'https://example.com/products/ikea_kallax.jpg', 1),
('绿萝 水培植物盆栽', '室内净化空气，好养易活，送礼自用皆宜，含水培瓶', 31, 49.90, 800, 5600, 'https://example.com/products/pothos.jpg', 1),
('相框组合 6英寸*6张套装', '实木边框，多款摆台/挂墙两用，照片墙装饰首选', 31, 89.00, 500, 3200, 'https://example.com/products/photo_frames6.jpg', 1),
('北欧风香薰蜡烛 礼盒套装', '大豆蜡，天然精油，木芯烛芯，低烟环保，送礼佳品', 31, 129.00, 300, 1800, 'https://example.com/products/nordic_candle.jpg', 1),

-- ===== 图书文具 (category_id=33/34/35) =====
('《三体》全套3册 刘慈欣', '雨果奖获奖作品，中国科幻巅峰之作，精装典藏版', 33, 129.00, 500, 4500, 'https://example.com/products/book_santi.jpg', 1),
('《活着》余华著 精装版', '当代文学经典，感人至深，多次重印畅销小说', 33, 49.00, 600, 5800, 'https://example.com/products/book_huozhe.jpg', 1),
('《百年孤独》马尔克斯 新版', '魔幻现实主义巨著，诺贝尔文学奖作品，典藏精装', 33, 59.00, 400, 3200, 'https://example.com/products/book_100years.jpg', 1),
('《深度学习》花书 中文版', '深度学习领域权威教材，Ian Goodfellow著，AI必读', 34, 199.00, 200, 1200, 'https://example.com/products/book_deeplearning.jpg', 1),
('《高中数学全套教辅》', '全国通用，同步练习+解析，学霸笔记整理版', 34, 89.00, 400, 2800, 'https://example.com/products/book_math.jpg', 1),
('晨光 大容量中性笔 0.5mm 20支', '快干墨水，顺滑书写，子弹头笔尖，学生办公两用', 35, 29.90, 1000, 12000, 'https://example.com/products/mg_pen20.jpg', 1),
('国誉 Campus 活页本 B5 80页', '便于携带，可拆分，80g纸张，横线/方格可选', 35, 19.90, 800, 8500, 'https://example.com/products/kokuyo_b5.jpg', 1),
('飞利浦 A4护眼台灯', '无蓝光，无频闪，自动感光，5档亮度，USB快充', 35, 299.00, 300, 1600, 'https://example.com/products/philips_lamp.jpg', 1),

-- ===== 健身器材 (category_id=37) =====
('小米 体脂秤 2 Pro', '蓝牙连接，27项健康数据，体脂率/BMI/心率监测', 37, 149.00, 600, 4800, 'https://example.com/products/mi_scale2pro.jpg', 1),
('哑铃组合套装 10kg*2', '铸铁包胶，六角防滚，家用健身首选，附收纳架', 37, 299.00, 200, 1100, 'https://example.com/products/dumbbell10.jpg', 1),
('跑步机 家用折叠款 8km/h', '静音减震，12档坡度，心率监测，折叠节省空间', 37, 1999.00, 80, 320, 'https://example.com/products/treadmill_home.jpg', 1),
('弹力带套装 5条不同阻力', '天然乳胶，5种阻力，健身/康复/瑜伽多用途', 37, 89.00, 500, 3500, 'https://example.com/products/resistance_bands.jpg', 1),
('瑜伽垫 加厚10mm 防滑', '天然橡胶，双面防滑，183*61cm，附绑带和背包', 37, 179.00, 400, 2800, 'https://example.com/products/yogamat10mm.jpg', 1),

-- ===== 户外装备 (category_id=38) =====
('北面 双肩登山包 45L', '防泼水面料，人体工学背板，雨罩内置，旅行徒步两用', 38, 1299.00, 100, 450, 'https://example.com/products/tnf_45l.jpg', 1),
('牧高笛 双人帐篷 3000mm防水', '铝合金支架，速开设计，3000mm防水，四季通用', 38, 899.00, 80, 320, 'https://example.com/products/mobi_tent2p.jpg', 1),
('探路者 男士冲锋衣', '三合一设计，防风防水，可拆卸内胆，多口袋设计', 38, 899.00, 150, 680, 'https://example.com/products/tpfs_jacekt.jpg', 1),

-- ===== 球类运动 (category_id=39) =====
('耐克 7号篮球 室内外兼用', 'Fuse 8P宽沟设计，持久手感，橡胶底层，比赛级别', 39, 299.00, 300, 1800, 'https://example.com/products/nike_basketball.jpg', 1),
('Wilson 网球拍初学者套装', 'Ultra 108全碳素，含3颗网球，轻量设计，男女老少皆宜', 39, 399.00, 150, 680, 'https://example.com/products/wilson_tennis.jpg', 1),
('迪卡侬 羽毛球拍 双拍套装', '铝碳合金，含球2筒，轻巧耐用，入门/娱乐首选', 39, 199.00, 400, 2600, 'https://example.com/products/decathlon_badminton.jpg', 1),
('斯伯丁 5号足球 训练款', 'PU革面，内层尼龙，标准重量，多场地适用', 39, 199.00, 250, 1200, 'https://example.com/products/spalding_soccer5.jpg', 1),

-- ===== 护肤品 (category_id=41) =====
('SK-II 神仙水 230ml', 'Pitera精华，改善肌肤纹理，提亮肤色，日本进口', 41, 1080.00, 150, 760, 'https://example.com/products/skii_230.jpg', 1),
('雅诗兰黛 小棕瓶精华 50ml', '双效修护，抗氧化，睡前精华，夜间修复肌肤', 41, 799.00, 200, 980, 'https://example.com/products/estee_50ml.jpg', 1),
('玉兰油 多效修护霜 50g', '大红瓶经典款，保湿修护，淡斑紧致，国民护肤品', 41, 189.00, 500, 3800, 'https://example.com/products/olay_cream.jpg', 1),
('百雀羚 3倍水肌肤修护套装', '控油补水，三重透明质酸，平价好用，国货之光', 41, 99.00, 600, 5600, 'https://example.com/products/bqling_set.jpg', 1),
('欧莱雅 玻尿酸安瓶精华 7支', '高浓度玻尿酸，一周焕肤体验，便携安瓶设计', 41, 89.00, 800, 7200, 'https://example.com/products/loreal_ampoule.jpg', 1),
('理肤泉 特安舒缓乳 40ml', '镇静敏感，修护屏障，医美后护肤，无香精配方', 41, 89.00, 400, 3200, 'https://example.com/products/laroche_40ml.jpg', 1),

-- ===== 彩妆 (category_id=42) =====
('MAC 子弹头口红 M·A·C子弹', '多种色号，显色持久，奶油质地，经典时尚', 42, 210.00, 300, 1800, 'https://example.com/products/mac_lipstick.jpg', 1),
('花西子 百鸟朝凤眼影盘', '东方彩妆，10色眼影，哑光+珠光，显色度高', 42, 199.00, 250, 1450, 'https://example.com/products/florasis_eye.jpg', 1),
('兰蔻 菁纯粉底液 SPF10', '持久遮瑕，自然哑光，多色号，全天轻薄感', 42, 420.00, 120, 540, 'https://example.com/products/lancome_foundation.jpg', 1),
('完美日记 探险家眼影盘', '12色多用，高颜值包装，显色持久，平替彩妆', 42, 79.00, 600, 5200, 'https://example.com/products/pday_eye12.jpg', 1),

-- ===== 香水 (category_id=43) =====
('香奈儿 5号香水 50ml', '花香醛香调，经典中的经典，礼盒包装，送礼首选', 43, 980.00, 80, 320, 'https://example.com/products/chanel5_50ml.jpg', 1),
('迪奥 旷野男士香水 100ml', '木质麝香，清新自然，长达8小时留香', 43, 850.00, 100, 420, 'https://example.com/products/dior_sauvage.jpg', 1),
('祖马龙 蓝风铃淡香水 30ml', '英国小众香，清新铃兰，适合日常佩戴', 43, 550.00, 60, 210, 'https://example.com/products/jomalone_30ml.jpg', 1),

-- ===== 婴幼儿用品 (category_id=45) =====
('好奇 铂金装纸尿裤 L码 60片', '亲肤内层，360°腰围设计，透气防漏，12h不湿', 45, 139.00, 400, 3800, 'https://example.com/products/huggies_l60.jpg', 1),
('帮宝适 超薄干爽纸尿裤 XL 56片', '超薄设计，瞬吸干爽，防侧漏设计，宝宝睡觉不红屁股', 45, 129.00, 500, 4200, 'https://example.com/products/pampers_xl56.jpg', 1),
('贝亲 婴儿洗发沐浴二合一 500ml', '弱酸性配方，温和无泪，植物来源表活，新生儿适用', 45, 69.00, 600, 5000, 'https://example.com/products/pigeon_wash500.jpg', 1),
('爱他美 经典版奶粉 3段 800g', '德国产，A2奶源，益生菌+DHA，适合1-3岁宝宝', 45, 369.00, 200, 1200, 'https://example.com/products/aptamil_800g.jpg', 1),
('曼龙 婴儿推车 可坐可躺', '轻量折叠，0-3岁适用，双向推行，一键收折', 45, 1899.00, 60, 230, 'https://example.com/products/manlun_stroller.jpg', 1),

-- ===== 儿童玩具 (category_id=46) =====
('乐高 城市系列 消防局', '1,088粒，适合6-12岁，锻炼动手能力，亲子互动', 46, 699.00, 150, 680, 'https://example.com/products/lego_fire.jpg', 1),
('万代 高达模型 元祖高达 MG 1/100', '精密零件，多色可选，展示珍藏两相宜', 46, 299.00, 100, 450, 'https://example.com/products/bandai_gundam.jpg', 1),
('奥迪双钻 遥控车 越野四驱', '四驱越野，车速可调，充电续航，儿童生日礼物', 46, 199.00, 300, 1600, 'https://example.com/products/auldey_car.jpg', 1),
('费雪 宝宝健身架 0-1岁', '多感官刺激，音乐+灯光，可拆卸牙胶玩具，益智早教', 46, 299.00, 200, 1200, 'https://example.com/products/fisher_gym.jpg', 1),

-- ===== 汽车电子 (category_id=48) =====
('行车记录仪 4K双镜头', '前后双录，夜视增强，ADAS预警，停车监控，APP互联', 48, 599.00, 200, 1100, 'https://example.com/products/dashcam_4k.jpg', 1),
('70迈 流媒体后视镜 行车记录仪', '10英寸大屏，前后双录，倒车影像，语音控制', 48, 799.00, 150, 760, 'https://example.com/products/70mai_mirror.jpg', 1),
('小米 车载充电器 100W氮化镓', '100W快充，双USB-A+双USB-C，过温过流保护', 48, 99.00, 600, 4800, 'https://example.com/products/mi_carcharger.jpg', 1),
('Mobileye 辅助驾驶系统 8s', '碰撞预警，车道偏离，跟车过近提醒，新手必备', 48, 2999.00, 30, 90, 'https://example.com/products/mobileye_8s.jpg', 1),

-- ===== 汽车内饰 (category_id=49) =====
('汽车香薰 车载固体香膏', '持久留香，多种香型，纯天然植物萃取，无酒精', 49, 49.00, 800, 6500, 'https://example.com/products/car_perfume.jpg', 1),
('汽车坐垫 四季通用透气', '3D透气网格，舒适防滑，一套四件，保护原车座椅', 49, 299.00, 300, 1600, 'https://example.com/products/car_cushion.jpg', 1),
('汽车挂件 翡翠葫芦吊坠', '天然翡翠，精工雕刻，新车装饰，寓意吉祥', 49, 199.00, 200, 980, 'https://example.com/products/jade_gourd.jpg', 1),

-- ===== 汽车养护 (category_id=50) =====
('嘉实多 极护 5W-40 全合成机油 4L', 'API SP认证，钛流体技术，磨损保护，适合大众丰田本田', 50, 299.00, 300, 1800, 'https://example.com/products/castrol_4l.jpg', 1),
('博世 雨刷 前后套装', '天然橡胶，360°贴合，无骨设计，耐磨耐候', 50, 89.00, 600, 4200, 'https://example.com/products/bosch_wiper.jpg', 1),
('汽车轮胎 米其林 205/55 R16', '米其林驾驭系列，低滚阻，静音舒适，湿地制动优秀', 50, 699.00, 100, 450, 'https://example.com/products/michelin_205.jpg', 1),

-- ===== 猫咪用品 (category_id=51) =====
('猫粮 皇家成猫猫粮 2kg', '高蛋白质配方，泌尿道健康维护，1岁以上适用', 51, 99.00, 400, 3200, 'https://example.com/products/royal_cat2kg.jpg', 1),
('猫抓板 瓦楞纸双面 60cm', '耐磨材质，猫薄荷香，保护家具，附赠玩具球', 51, 29.00, 800, 7500, 'https://example.com/products/cat_scratcher.jpg', 1),
('猫玩具套装 逗猫棒*3+老鼠球*5', '多种互动玩具，解压好物，增进人宠感情', 51, 39.00, 600, 5000, 'https://example.com/products/cat_toys.jpg', 1),
('自动猫砂盆 智能清洁', '自动铲屎，密封除臭，APP远程控制，大容量', 51, 1299.00, 80, 380, 'https://example.com/products/auto_litter.jpg', 1),

-- ===== 狗狗用品 (category_id=52) =====
('皇家 中型犬成犬狗粮 4kg', '均衡营养，维护关节健康，大颗粒设计减少牙垢', 52, 189.00, 300, 2100, 'https://example.com/products/royal_dog4kg.jpg', 1),
('牵引绳 胸背一体式 防暴冲', '背带+项圈二合一，强力固定，反光条设计，夜间安全', 52, 99.00, 400, 2800, 'https://example.com/products/dog_leash.jpg', 1),
('狗狗牙刷套装 360度', '360度清洁，食品级硅胶，狗狗爱用，预防口腔疾病', 52, 49.00, 500, 3600, 'https://example.com/products/dog_toothbrush.jpg', 1),

-- ===== 宠物食品 (category_id=53) =====
('皇家 猫用幼猫奶糕 400g', '0-4月幼猫专用，高蛋白，含益生菌，促进肠胃健康', 53, 79.00, 300, 1800, 'https://example.com/products/royal_kitten400.jpg', 1),
('麦富迪 鸡肉风味零食 100g', '冻干鸡肉，无添加，高蛋白，猫狗通用', 53, 39.00, 600, 5200, 'https://example.com/products/mfd_chicken100.jpg', 1),
('比瑞吉 天然粮 幼犬狗粮 7.5kg', '无谷配方，新鲜鸡肉，益生元，支持免疫和消化', 53, 239.00, 200, 1100, 'https://example.com/products/bridge_puppy75.jpg', 1);

-- ========================================
-- 用户评分（保证协同过滤算法有足够重叠数据可计算）
-- ========================================
INSERT INTO ratings (user_id, product_id, score, comment) VALUES
(2, 1, 4.5, '手机很好用,系统流畅'),
(2, 2, 5.0, '手感出色,续航给力'),
(2, 3, 4.0, '信号好,拍照强'),
(2, 4, 3.5, '性价比不错'),
(2, 11, 5.0, '性能强劲,屏幕超棒'),
(3, 1, 5.0, '超级喜欢!'),
(3, 5, 4.0, '拍照很好'),
(3, 6, 3.0, '一般般'),
(3, 2, 4.5, '工作利器'),
(3, 12, 4.0, '轻薄便携');

-- ========================================
-- 用户行为（浏览/收藏/加购）
-- ========================================
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
-- 验证插入结果
-- ========================================
SELECT COUNT(*) AS total_categories FROM categories;
SELECT COUNT(*) AS total_products FROM products;
SELECT c.name AS category_name, COUNT(p.id) AS product_count
FROM categories c
LEFT JOIN products p ON p.category_id = c.id
WHERE c.level = 2
GROUP BY c.id, c.name
ORDER BY c.parent_id, c.id;

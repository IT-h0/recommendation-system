"""
批量生成 recommendation_system 各表测试数据
覆盖：users / ratings / user_behavior / cart_items / orders / order_items
"""
import random
import datetime
import mysql.connector

# ── 数据库连接 ──────────────────────────────────────────────
conn = mysql.connector.connect(
    host='localhost', port=3306,
    user='root', password='1270039609Aa',
    database='recommendation_system',
    charset='utf8mb4'
)
cur = conn.cursor()

# ── 工具函数 ────────────────────────────────────────────────
def rand_dt(days_ago_max=365, days_ago_min=1):
    """随机生成过去某天的 datetime"""
    delta = random.randint(days_ago_min, days_ago_max)
    base = datetime.datetime.now() - datetime.timedelta(days=delta)
    base += datetime.timedelta(
        hours=random.randint(0, 23),
        minutes=random.randint(0, 59),
        seconds=random.randint(0, 59)
    )
    return base.strftime('%Y-%m-%d %H:%M:%S')

# BCrypt hash of "user123"（用于所有测试用户）
USER_PWD_HASH = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi'

# ════════════════════════════════════════════════════════════
# 1. 用户数据（新增 20 个用户，总计≈24）
# ════════════════════════════════════════════════════════════
print(">>> 插入用户数据 ...")

nicknames = [
    '数码达人', '购物狂人', '美食控', '时尚博主', '健身教练',
    '读书人', '宅家族', '旅行者', '摄影师', '游戏玩家',
    '职场白领', '学生党', '宝妈', '宝爸', '美妆达人',
    '运动达人', '极客', '厨师', '爱猫人士', '汽车爱好者'
]

users_sql = """
INSERT IGNORE INTO users (username, password, email, phone, nickname, status, created_time)
VALUES (%s, %s, %s, %s, %s, 1, %s)
"""
users_data = []
for i in range(1, 21):
    users_data.append((
        f'testuser{i:02d}',
        USER_PWD_HASH,
        f'testuser{i:02d}@example.com',
        f'138{i:08d}',
        nicknames[i - 1],
        rand_dt(365, 30)
    ))
cur.executemany(users_sql, users_data)
conn.commit()
print(f"    用户插入完成，新增 {cur.rowcount} 条")

# 获取所有用户 ID
cur.execute("SELECT id FROM users")
all_user_ids = [r[0] for r in cur.fetchall()]

# 获取所有商品（id + price）
cur.execute("SELECT id, price FROM products WHERE status = 1")
all_products = cur.fetchall()   # [(id, price), ...]
all_product_ids = [p[0] for p in all_products]
product_price = {p[0]: float(p[1]) for p in all_products}

print(f"    可用用户 {len(all_user_ids)} 个，可用商品 {len(all_product_ids)} 个")

# ════════════════════════════════════════════════════════════
# 2. 评分数据（ratings）
#    每个用户对 10~30 件商品评分，确保唯一
# ════════════════════════════════════════════════════════════
print(">>> 插入评分数据 ...")

# 先清掉原来的少量测试数据，重新生成
cur.execute("DELETE FROM ratings")
conn.commit()

SCORES = [1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0]
COMMENTS = [
    '非常好用，强烈推荐！', '质量一般，不太满意。', '物超所值，下次还买！',
    '包装精美，送礼首选。', '发货很快，满意。', '和描述基本符合，还行。',
    '用了一段时间，感觉不错。', '性价比很高，值得购买！', '有点小失望，但还能接受。',
    '超出预期，非常满意！', '一般般，没什么惊喜。', '品质很好，做工精细。',
    '颜值颇高，用着也舒服。', '没啥大问题，值这个价。', '推荐给朋友，都说好。',
    '第二次购买，依然满意。', '略微有点贵，但品质在线。', '功能齐全，使用方便。',
    '物流很快，商品完好无损。', '有点小瑕疵，整体还OK。'
]

ratings_sql = """
INSERT INTO ratings (user_id, product_id, score, comment, created_time)
VALUES (%s, %s, %s, %s, %s)
"""
ratings_data = []
for uid in all_user_ids:
    sample_count = random.randint(10, 30)
    sampled_pids = random.sample(all_product_ids, min(sample_count, len(all_product_ids)))
    for pid in sampled_pids:
        score = random.choice(SCORES)
        comment = random.choice(COMMENTS) if random.random() > 0.3 else None
        ratings_data.append((uid, pid, score, comment, rand_dt(300, 1)))

cur.executemany(ratings_sql, ratings_data)
conn.commit()
print(f"    评分插入完成，共 {len(ratings_data)} 条")

# ════════════════════════════════════════════════════════════
# 3. 用户行为数据（user_behavior）
#    行为类型：1-浏览 2-收藏 3-加购 4-购买
#    浏览最多，购买最少
# ════════════════════════════════════════════════════════════
print(">>> 插入用户行为数据 ...")

cur.execute("DELETE FROM user_behavior")
conn.commit()

behavior_sql = """
INSERT INTO user_behavior (user_id, product_id, behavior_type, duration, created_time)
VALUES (%s, %s, %s, %s, %s)
"""
behavior_data = []
for uid in all_user_ids:
    # 浏览：30~60 件
    browse_pids = random.sample(all_product_ids, min(random.randint(30, 60), len(all_product_ids)))
    for pid in browse_pids:
        duration = random.randint(10, 600)  # 10秒~10分钟
        behavior_data.append((uid, pid, 1, duration, rand_dt(180, 1)))

    # 收藏：5~15 件（从浏览过的里选）
    fav_pids = random.sample(browse_pids, min(random.randint(5, 15), len(browse_pids)))
    for pid in fav_pids:
        behavior_data.append((uid, pid, 2, 0, rand_dt(150, 1)))

    # 加购：3~10 件
    cart_pids = random.sample(fav_pids, min(random.randint(3, 10), len(fav_pids)))
    for pid in cart_pids:
        behavior_data.append((uid, pid, 3, 0, rand_dt(120, 1)))

    # 购买：1~5 件
    buy_pids = random.sample(cart_pids, min(random.randint(1, 5), len(cart_pids)))
    for pid in buy_pids:
        behavior_data.append((uid, pid, 4, 0, rand_dt(90, 1)))

cur.executemany(behavior_sql, behavior_data)
conn.commit()
print(f"    用户行为插入完成，共 {len(behavior_data)} 条")

# ════════════════════════════════════════════════════════════
# 4. 购物车数据（cart_items）
#    每个用户 1~8 件未结算商品
# ════════════════════════════════════════════════════════════
print(">>> 插入购物车数据 ...")

cur.execute("DELETE FROM cart_items")
conn.commit()

cart_sql = """
INSERT IGNORE INTO cart_items (user_id, product_id, quantity, created_time)
VALUES (%s, %s, %s, %s)
"""
cart_data = []
for uid in all_user_ids:
    cart_pids = random.sample(all_product_ids, random.randint(1, 8))
    for pid in cart_pids:
        qty = random.randint(1, 3)
        cart_data.append((uid, pid, qty, rand_dt(30, 1)))

cur.executemany(cart_sql, cart_data)
conn.commit()
print(f"    购物车插入完成，共 {cur.rowcount} 条（IGNORE重复）")

# ════════════════════════════════════════════════════════════
# 5. 订单 + 订单详情（orders + order_items）
#    每个用户 2~8 笔历史订单，每笔 1~4 个商品
# ════════════════════════════════════════════════════════════
print(">>> 插入订单数据 ...")

cur.execute("DELETE FROM order_items")
cur.execute("DELETE FROM orders")
conn.commit()

order_sql = """
INSERT INTO orders (order_no, user_id, total_amount, status,
    receiver_name, receiver_phone, receiver_address, created_time)
VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
"""
order_item_sql = """
INSERT INTO order_items (order_id, product_id, product_name, product_image,
    price, quantity, total_price)
VALUES (%s, %s, %s, %s, %s, %s, %s)
"""

# 获取商品名称和图片
cur.execute("SELECT id, name, image_url, price FROM products WHERE status = 1")
products_info = {r[0]: {'name': r[1], 'image': r[2], 'price': float(r[3])} for r in cur.fetchall()}

STATUS_WEIGHTS = [
    (0, 5),   # 待支付 5%
    (1, 20),  # 已支付 20%
    (2, 20),  # 已发货 20%
    (3, 50),  # 已完成 50%
    (4, 5),   # 已取消 5%
]
statuses, weights = zip(*STATUS_WEIGHTS)

ADDRESSES = [
    '北京市朝阳区建国路88号', '上海市浦东新区陆家嘴环路1000号',
    '广州市天河区天河路385号', '深圳市南山区科技园南区',
    '杭州市西湖区文三路477号', '成都市高新区天府大道北段1480号',
    '武汉市洪山区光谷大道62号', '南京市鼓楼区中山路228号',
    '西安市雁塔区高新路35号', '重庆市渝北区星光大道65号'
]

order_counter = 1
orders_inserted = 0
items_inserted = 0

for uid in all_user_ids:
    order_count = random.randint(2, 8)
    for _ in range(order_count):
        order_no = f'ORD{datetime.datetime.now().strftime("%Y%m%d")}{order_counter:06d}'
        order_counter += 1
        order_dt = rand_dt(365, 1)
        status = random.choices(statuses, weights=weights)[0]

        # 选 1~4 件商品
        item_pids = random.sample(all_product_ids, random.randint(1, 4))
        total_amount = 0.0
        items_for_this_order = []
        for pid in item_pids:
            qty = random.randint(1, 3)
            price = products_info[pid]['price']
            subtotal = round(price * qty, 2)
            total_amount += subtotal
            items_for_this_order.append((pid, qty, price, subtotal))
        total_amount = round(total_amount, 2)

        # 插入订单
        cur.execute(order_sql, (
            order_no, uid, total_amount, status,
            f'收货人{uid}', f'138{uid:08d}',
            random.choice(ADDRESSES), order_dt
        ))
        order_id = cur.lastrowid
        orders_inserted += 1

        # 插入订单详情
        for pid, qty, price, subtotal in items_for_this_order:
            p = products_info[pid]
            cur.execute(order_item_sql, (
                order_id, pid, p['name'], p['image'],
                price, qty, subtotal
            ))
            items_inserted += 1

conn.commit()
print(f"    订单插入完成，共 {orders_inserted} 笔订单，{items_inserted} 条订单详情")

# ════════════════════════════════════════════════════════════
# 汇总
# ════════════════════════════════════════════════════════════
print("\n>>> 最终各表数据量：")
tables = ['users','categories','products','ratings','user_behavior','cart_items','orders','order_items','recommendations']
for t in tables:
    cur.execute(f"SELECT COUNT(*) FROM {t}")
    cnt = cur.fetchone()[0]
    print(f"    {t:<20} {cnt:>6} 条")

cur.close()
conn.close()
print("\n[完成] 全部数据插入成功！")

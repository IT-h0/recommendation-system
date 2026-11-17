# 基于协同过滤算法的电子商品推荐系统

一个完整的电商推荐系统,采用Spring Boot + MySQL + MyBatis构建后端,Vue3 + Element Plus构建前端,实现了基于物品的协同过滤推荐算法。

## 技术栈

### 后端
- **框架**: Spring Boot 3.2.0
- **数据库**: MySQL 8.0
- **ORM**: MyBatis 3.0.3
- **连接池**: Druid 1.2.20
- **缓存**: Redis
- **认证**: Spring Security + JWT
- **API文档**: Knife4j (Swagger)
- **工具库**: Hutool

### 前端
- **框架**: Vue 3.4.0
- **构建工具**: Vite 5.0
- **UI组件**: Element Plus 2.5.0
- **路由**: Vue Router 4.2.5
- **状态管理**: Pinia 2.1.7
- **HTTP客户端**: Axios 1.6.2

## 系统架构

### 多模块设计
- **recommendation-common**: 公共模块,提供基础能力(常量、工具类、配置)
- **recommendation-pojo**: 实体模块,定义实体类、DTO、VO
- **recommendation-server**: 服务模块,核心业务逻辑、API接口

### 核心功能
1. 用户管理:注册、登录、个人信息
2. 商品管理:商品列表、商品详情、分类查询
3. 评分系统:用户评分、评分查询
4. 购物车:添加商品、查看购物车、删除商品
5. 推荐系统:基于物品的协同过滤算法推荐

## 推荐算法

### 基于物品的协同过滤(Item-based Collaborative Filtering)

算法流程:
1. 构建用户-商品评分矩阵
2. 计算商品之间的相似度(余弦相似度)
3. 根据用户历史评分和商品相似度,预测用户对未评分商品的偏好
4. 按预测得分排序,返回TopN推荐

相似度计算公式:
```
sim(i, j) = cos(i, j) = (i · j) / (||i|| * ||j||)
```

## 数据库设计

### 主要表结构
- **users**: 用户表
- **products**: 商品表
- **categories**: 商品分类表
- **ratings**: 用户评分表
- **user_behavior**: 用户行为表
- **orders**: 订单表
- **order_items**: 订单详情表
- **cart_items**: 购物车表
- **recommendations**: 推荐结果表

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 5.0+
- Node.js 16+

### 后端启动

1. 创建数据库
```bash
mysql -u root -p < sql/schema.sql
```

2. 修改配置文件
编辑 `recommendation-server/src/main/resources/application.yml`,修改数据库和Redis密码:
```yaml
spring:
  datasource:
    password: 1270039609Aa  # 你的MySQL密码
  data:
    redis:
      password: 123456  # 你的Redis密码
```

3. 编译并运行
```bash
cd recommendation-system
mvn clean install
cd recommendation-server
mvn spring-boot:run
```

4. 访问API文档
```
http://localhost:8080/doc.html
```

### 前端启动

1. 安装依赖
```bash
cd frontend
npm install
```

2. 启动开发服务器
```bash
npm run dev
```

3. 访问应用
```
http://localhost:3000
```

## API接口文档

启动后端后,访问 `http://localhost:8080/doc.html` 查看完整的API文档。

### 主要接口
- POST `/api/user/login` - 用户登录
- POST `/api/user/register` - 用户注册
- GET `/api/product/list` - 获取商品列表
- GET `/api/product/{id}` - 获取商品详情
- POST `/api/rating` - 用户评分
- GET `/api/recommendation` - 获取推荐商品
- GET `/api/cart` - 获取购物车
- POST `/api/cart` - 添加商品到购物车

## 测试账号

- 管理员: admin / admin123
- 测试用户1: user1 / user123
- 测试用户2: user2 / user123

## 项目结构

```
recommendation-system/
├── sql/                          # 数据库脚本
│   └── schema.sql
├── recommendation-common/         # 公共模块
│   └── src/main/java/com/recommendation/common/
│       ├── constants/           # 常量
│       ├── enums/               # 枚举
│       ├── exception/           # 异常
│       ├── result/              # 响应结果
│       ├── util/                # 工具类
│       ├── properties/          # 配置属性
│       └── config/              # 配置类
├── recommendation-pojo/          # 实体模块
│   └── src/main/java/com/recommendation/pojo/
│       ├── entity/              # 实体类
│       ├── dto/                 # 数据传输对象
│       └── vo/                  # 视图对象
├── recommendation-server/        # 服务模块
│   └── src/main/
│       ├── java/com/recommendation/server/
│       │   ├── config/          # 配置类
│       │   ├── controller/      # 控制器
│       │   ├── service/         # 服务层
│       │   ├── mapper/          # 数据访问层
│       │   ├── security/        # 安全配置
│       │   └── algorithm/       # 推荐算法
│       └── resources/
│           ├── mapper/          # MyBatis映射文件
│           └── application.yml  # 应用配置
└── frontend/                     # 前端项目
    ├── src/
    │   ├── api/                # API接口
    │   ├── assets/             # 静态资源
    │   ├── components/         # 组件
    │   ├── layouts/            # 布局
    │   ├── router/             # 路由
    │   ├── stores/             # 状态管理
    │   ├── views/              # 页面
    │   ├── App.vue             # 根组件
    │   └── main.js             # 入口文件
    ├── index.html
    ├── package.json
    └── vite.config.js
```

## 核心算法说明

### 余弦相似度计算

```java
// 计算两个商品的余弦相似度
BigDecimal similarity = calculateCosineSimilarity(userItemMatrix, item1, item2);
```

算法实现位置: `recommendation-server/src/main/java/com/recommendation/server/algorithm/CollaborativeFiltering.java`

### 推荐流程

1. 获取所有用户的评分数据
2. 构建用户-商品评分矩阵
3. 计算商品之间的相似度矩阵
4. 对每个未评分商品,计算预测得分
5. 返回得分最高的N个商品

## 注意事项

1. 默认数据库密码为 `1270039609Aa`,Redis密码为 `123456`,请根据实际情况修改
2. 首次使用需要先执行数据库脚本
3. 推荐算法需要一定的评分数据才能生效,可以先手动添加一些评分记录
4. 前端开发环境默认代理到后端 `http://localhost:8080`

## 后续优化方向

1. 引入更复杂的推荐算法(矩阵分解、深度学习等)
2. 实现实时推荐更新
3. 添加冷启动处理策略
4. 优化算法性能,支持大规模数据
5. 添加A/B测试功能
6. 完善订单系统和支付功能

## 许可证

MIT License

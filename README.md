# 基于 AI 的智能电商推荐平台

> 一个融合 **协同过滤** 与 **大模型智能导购** 的电商推荐系统：用户既可以通过评分数据获得个性化推荐，也可以用**自然语言对话**直接获得精准、附带理由的商品推荐。

## 项目简介

本项目是一个完整的电商推荐系统，后端采用 Spring Boot 多模块架构，前端采用 Vue3 + Element Plus。在传统电商「搜索 + 筛选」模式之外，重点实现了两套可独立或叠加运行的推荐引擎：

1. **Item-CF 协同过滤引擎**：基于用户-商品评分矩阵计算商品间相似度，预测用户偏好做个性化推荐；
2. **LLM 智能导购引擎**：接入 DeepSeek 大模型 + RAG 检索增强，通过 Function Calling 工具链让 LLM 自主决策调用，以 SSE 流式返回精准、附带理由的商品推荐。

## ✨ 核心亮点

- 🤖 **AI 智能导购**：自然语言对话式购物，LLM 自主决策调用工具，SSE 流式逐字输出推荐理由；
- 🔍 **RAG 混合检索**：Qdrant 向量语义召回 + 关键词召回 + 结构化过滤，RRF 融合重排 + 相关性阈值；
- 📊 **RAG 评测体系**：检索指标（Hit@K / MRR / NDCG@K）+ 生成质量（Faithfulness / Answer Relevance，LLM-as-Judge）；
- 🔧 **原生 HTTP 工具调用层**：自研 `DeepSeekClient`，用标准 `tools` 协议解决旧版 LangChain4j 与 DeepSeek 的 DSML 兼容问题；
- 🛡️ **工程化设计**：多模块解耦、Redis 分层缓存、Function Calling 安全注入、多轮上下文、结果去重封顶。

## 技术栈

### 后端

| 类别 | 技术 |
|---|---|
| 框架 | Spring Boot 3.2.0（Java 21） |
| 构建 | Maven 多模块（common / pojo / server） |
| ORM | MyBatis 3.0.3 |
| 连接池 | Druid 1.2.20 |
| 缓存 | Redis（热点缓存 / 分布式锁 / 会话） |
| 认证 | Spring Security + JWT |
| API 文档 | Knife4j（Swagger） |
| JSON | FastJSON2 |
| LLM 框架 | LangChain4j 1.0.0-beta2 |
| 工具库 | Hutool |

### AI 能力

| 类别 | 技术 |
|---|---|
| 大模型 | DeepSeek（`deepseek-flash`） |
| 向量嵌入 | 百炼 DashScope `text-embedding-v3`（1024 维） |
| 向量数据库 | Qdrant（Cosine 相似度） |

### 前端

| 类别 | 技术 |
|---|---|
| 框架 | Vue 3.4 |
| 构建工具 | Vite 5 |
| UI 组件 | Element Plus 2.5 |
| 路由 | Vue Router 4 |
| 状态管理 | Pinia 2 |
| HTTP 客户端 | Axios |

## 功能特性

- **用户管理**：注册、登录、JWT 认证、个人信息
- **商品管理**：商品列表、详情、分类查询、关键词搜索
- **分类管理**：分类列表、分类树
- **评分系统**：用户评分、商品评分查询
- **购物车**：添加、查看、删除、清空
- **订单系统**：下单、支付、取消、订单查询
- **推荐系统**：Item-CF 协同过滤推荐
- **AI 智能导购**：对话式商品推荐（RAG + Function Calling + SSE）

## 核心设计

### 1. 推荐引擎一：Item-CF 协同过滤

基于物品的协同过滤（Item-based Collaborative Filtering），流程如下：

1. 构建用户-商品评分矩阵；
2. 用**余弦相似度**计算商品之间的相似度；
3. 根据用户历史评分与商品相似度，预测用户对未评分商品的偏好；
4. 按预测得分排序，返回 Top-N 推荐。

相似度公式：

```
sim(i, j) = cos(i, j) = (i · j) / (‖i‖ · ‖j‖)
```

实现位置：`recommendation-server/.../algorithm/CollaborativeFiltering.java`

### 2. 推荐引擎二：LLM 智能导购（RAG）

用户用自然语言描述需求（如「预算 5000 推荐几款手机」），后端通过 **DeepSeek + Function Calling** 完成检索与推荐，执行链路：

```
用户输入 → 构建 system prompt + 历史上下文
        → DeepSeek 原生 HTTP 调用（携带 tools 定义）
        → LLM 返回 tool_calls（search_products / get_product_detail / get_recommendations）
        → 执行工具（RAG 混合检索 / 协同过滤 / 商品详情）
        → 工具结果回填上下文，LLM 生成最终回答
        → SSE 先推商品卡片事件，再流式推文字，最后 done
```

关键设计：

- **原生 HTTP 工具调用层**（`DeepSeekClient`）：直连 DeepSeek `/chat/completions` 并使用标准 `tools` 参数，返回标准 JSON `tool_calls`，彻底解决旧版 LangChain4j 与 DeepSeek 的 DSML 格式不兼容问题；
- **三个工具**：`search_products`（关键词/分类/价格搜索）、`get_product_detail`（商品详情）、`get_recommendations`（协同过滤推荐）；
- **SSE 流式输出**：商品卡片先渲染、文字逐字输出，前端基于 `ReadableStream` 消费 SSE；
- **多轮上下文**：助手消息附带「上一轮推荐商品的 productId」，支持「说说 XX 的参数」这类追问；
- **结果去重 + 封顶 3 件**：多次工具调用结果按 productId 去重，最多展示 3 张商品卡片；
- **安全设计**：`FunctionRegistry` 统一注入 `userId`，防止 LLM 越权查询其他用户数据；
- **失败降级**：向量检索不可用时自动回退关键词/全库检索，AI 导购整体不中断。

### 3. RAG 混合检索

智能导购的检索核心是**混合检索**，解决单一检索方式召回不准的问题：

| 检索路 | 方式 | 作用 |
|---|---|---|
| 稠密向量召回 | Qdrant（Cosine）+ DashScope `text-embedding-v3`（1024 维） | 语义匹配（「怕冷的外套」≈「加厚羽绒服」） |
| 稀疏关键词召回 | SQL `LIKE` | 字面精确匹配（型号、品牌词） |
| 结构化过滤 | 分类 + 价格区间 | 约束搜索空间 |

三路结果取并集后，用 **RRF（Reciprocal Rank Fusion，倒数排名融合）** 按名次融合排序，避免向量分（0~1）与关键词分（无界）量纲不一致；再叠加**相关性阈值（0.2）**过滤明显不相关商品，最后按需取 Top-N（封顶 3 件）。

详细架构与时序图见 [ARCHITECTURE.md](ARCHITECTURE.md)。

### 4. RAG 评测体系

为量化检索与生成质量，项目内置了一套评测体系：

**检索质量**（衡量「找得准不准、全不全、排得好不好」）：

- `Hit@K`：前 K 个结果是否至少命中一个相关商品；
- `Precision@K` / `Recall@K`：精确率 / 召回率；
- `MRR`：第一个相关商品排名的倒数均值；
- `NDCG@K`：位置折损累计增益，衡量排序质量。

**生成质量**（衡量「答得对不对、有没有瞎编」，用 LLM-as-Judge 打分）：

- `Faithfulness`（忠实度）：答案是否完全基于检索上下文、不编造；
- `Answer Relevance`（答案相关性）：答案是否切题。

> 实测（429 件商品索引）：检索 `Hit@10 = 100%`、`MRR = 0.86`、`NDCG@10 = 0.80`；生成 `Faithfulness = 0.967`、`Answer Relevance = 0.854`。

## 数据库设计

共 **11 张表**，覆盖用户、商品、评分、行为、订单、购物车、推荐、AI 对话等业务域：

| 表 | 说明 |
|---|---|
| `users` | 用户表 |
| `products` | 商品表（含语义标签、AI 摘要字段） |
| `categories` | 商品分类表（两级） |
| `ratings` | 用户评分表 |
| `user_behavior` | 用户行为表（浏览/收藏/加购/购买） |
| `orders` | 订单表 |
| `order_items` | 订单详情表 |
| `cart_items` | 购物车表 |
| `recommendations` | 推荐结果表 |
| `chat_sessions` | AI 对话会话表 |
| `chat_messages` | AI 对话消息表（含 function_name、metadata） |

建表脚本见 `sql/schema.sql`。

## 项目结构

```
recommendation-system/
├── sql/                          # 数据库脚本（schema.sql / data_insert.sql）
├── recommendation-common/         # 公共模块（常量/枚举/异常/结果/工具/配置属性）
├── recommendation-pojo/           # 实体模块（entity / dto / vo）
├── recommendation-server/         # 服务模块（核心业务 + API + AI）
│   └── src/main/
│       ├── java/com/recommendation/server/
│       │   ├── ai/                # AI 导购
│       │   │   ├── DeepSeekClient.java       # 原生 HTTP 工具调用层
│       │   │   ├── rag/                       # RAG 检索（Qdrant/嵌入/混合检索/RRF）
│       │   │   ├── eval/                      # 评测体系（检索/生成指标）
│       │   │   └── tool/                      # 工具定义（Function Calling）
│       │   ├── algorithm/         # Item-CF 协同过滤
│       │   ├── config/            # 配置类（Security/LangChain4j）
│       │   ├── controller/        # 控制器
│       │   ├── service/           # 服务层
│       │   ├── mapper/            # 数据访问层
│       │   └── security/          # 安全（JWT 过滤器等）
│       └── resources/
│           ├── mapper/            # MyBatis 映射文件
│           └── application*.yml   # 应用配置
└── frontend/                      # 前端项目（Vue3 + Element Plus）
    └── src/（api/components/views/stores/router/layouts）
```

## 快速开始

### 环境要求

- **JDK 21**（项目编译目标为 Java 21）
- Maven 3.6+
- MySQL 8.0+
- Redis（默认 `localhost:6379`）
- Qdrant 向量库（默认 `192.168.100.101:6333`）
- Node.js 16+（前端）

### 1. 初始化数据库

```bash
mysql -u root -p < sql/schema.sql
```

### 2. 配置

敏感配置（数据库密码、Redis 密码、DeepSeek / DashScope API Key）通过**环境变量**注入，本地开发可直接使用 `application.yml` 中的默认值，或复制 `.env.example` 为 `.env` 填入真实值。

```bash
# 方式一：直接改 application.yml 里的默认值
# recommendation-server/src/main/resources/application.yml

# 方式二：复制 .env.example 为 .env 并填写（.env 已被 gitignore）
cp .env.example .env
```

### 3. 启动后端

```bash
mvn clean install
cd recommendation-server
mvn spring-boot:run
```

启动后 API 文档：`http://localhost:8080/doc.html`

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问应用：`http://localhost:3000`

## API 接口

| 模块 | 接口 | 说明 |
|---|---|---|
| 用户 | `POST /api/user/login` | 登录 |
| 用户 | `POST /api/user/register` | 注册 |
| 用户 | `GET /api/user/info` | 当前用户信息 |
| 商品 | `GET /api/product/list` | 商品列表 |
| 商品 | `GET /api/product/{id}` | 商品详情 |
| 商品 | `GET /api/product/category/{categoryId}` | 按分类查商品 |
| 商品 | `GET /api/product/search` | 关键词搜索 |
| 分类 | `GET /api/category/list` | 分类列表 |
| 分类 | `GET /api/category/tree` | 分类树 |
| 评分 | `POST /api/rating` | 用户评分 |
| 评分 | `GET /api/rating/product/{productId}` | 商品评分 |
| 购物车 | `GET/POST /api/cart` | 查看/添加购物车 |
| 订单 | `POST /api/orders` | 下单 |
| 订单 | `GET /api/orders` | 订单列表 |
| 推荐 | `GET /api/recommendation` | Item-CF 推荐 |
| AI 导购 | `POST /api/chat/send` | 对话（SSE 流式） |
| AI 导购 | `GET /api/chat/sessions` | 会话列表 |
| AI 导购 | `GET /api/chat/sessions/{id}/messages` | 会话消息 |

## 测试账号

| 账号 | 密码 |
|---|---|
| admin | admin123 |
| user1 | user123 |
| user2 | user123 |

## 相关文档

- [ARCHITECTURE.md](ARCHITECTURE.md) — AI 导购架构图与时序图（Mermaid）
- [DEPLOYMENT.md](DEPLOYMENT.md) — 部署指南（含 Nginx / Docker）

## 许可证

MIT License

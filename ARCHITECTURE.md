# AI 导购（对话式购物 Agent）架构说明

> 技术栈：Spring Boot + LangChain4j + DeepSeek + 百炼 Embedding + Qdrant + MySQL + Redis
> 本文用 Mermaid 绘制，可在 [Mermaid Live](https://mermaid.live)、GitHub Markdown、Typora、draw.io 中渲染。

## 一、架构图（组件与数据流）

```mermaid
flowchart TB
    subgraph FE["前端 Vue3"]
        A["ChatAssistant.vue<br/>(SSE 流式渲染 + 商品卡片)"]
    end

    subgraph BE["后端 Spring Boot + LangChain4j"]
        B["ChatController<br/>(/api/chat/send)"]
        C["ChatServiceImpl<br/>(Agent 编排 · 工具循环 · 上下文管理)"]
        D["ShoppingAssistantTools<br/>(3 个工具)"]
        E["HybridProductSearchService<br/>(混合检索 + RRF 重排)"]
        F["RecommendationService<br/>(Item-CF 协同过滤)"]
        G["ProductService<br/>(商品查询/Redis 缓存)"]
        H["ProductContextBuilder<br/>(商品目录概览)"]
    end

    subgraph RAG["RAG 检索层"]
        I["ProductVectorIndex"]
        J["DashScopeTextEmbedder"]
        K["QdrantProductVectorStore"]
    end

    subgraph EXT["外部依赖"]
        L["DeepSeek LLM<br/>(同步模型 + 流式模型)"]
        M["百炼 Embedding<br/>(text-embedding-v3)"]
        N[("Qdrant 向量库")]
        O[("MySQL")]
        P[("Redis")]
    end

    A -->|"SSE"| B --> C
    C -->|"tool_calls"| D
    C -->|"system prompt"| H
    C -->|"对话(同步/流式)"| L
    D -->|"search_products"| E
    D -->|"get_recommendations"| F
    D -->|"get_product_detail"| G
    E --> I
    I --> J --> M
    I --> K --> N
    E -->|"关键词/批量查"| G
    G --> O
    G --> P
    F --> O
    C -->|"会话/消息持久化"| O
```

## 二、时序图（一次「帮我找复古墨镜」的完整调用）

```mermaid
sequenceDiagram
    autonumber
    participant U as 用户(前端)
    participant CC as ChatController
    participant CS as ChatServiceImpl
    participant LLM as DeepSeek(同步)
    participant ST as ShoppingAssistantTools
    participant RAG as HybridProductSearch
    participant QD as Qdrant
    participant DS as 百炼Embedding
    participant DB as MySQL

    U->>CC: POST /api/chat/send (SSE)
    CC->>CS: chat(userId, message)
    CS->>DB: 建/查会话 + 保存用户消息
    CS->>LLM: 第1轮 chat(上下文 + tools)
    LLM-->>CS: tool_calls: search_products("复古墨镜")
    CS->>ST: execute(search_products)
    ST->>RAG: search(keyword, category, price)
    RAG->>DS: embedQuery("复古墨镜")
    DS-->>RAG: 查询向量(1024维)
    RAG->>QD: 向量相似度搜索(top-50)
    QD-->>RAG: top-50 商品 + score
    RAG->>DB: 关键词 LIKE + 批量查商品
    DB-->>RAG: 商品列表
    RAG-->>ST: 结构化过滤 + RRF 重排 → TopN
    ST-->>CS: 工具结果(商品 JSON)
    CS->>LLM: 第2轮 chat(上下文 + 工具结果)
    LLM-->>CS: 无 tool_calls → 生成最终回答
    CS-->>U: SSE: product 事件(商品卡片先渲染)
    CS->>LLM: 流式模型输出
    LLM-->>CS: 逐 token
    CS-->>U: SSE: text 事件(文字流式)
    CS->>DB: 保存助手消息(含商品 metadata)
    CS-->>U: SSE: done 事件(带 sessionId)
```

## 三、关键设计点

1. **手写工具调用循环**（而非 AiServices）：为了拿到工具返回的商品数据、单独推 `product` SSE 事件。
2. **同步 + 流式双模型**：工具循环用同步模型（需同步拿 tool_calls 结果），最终回答用流式模型（逐 token）。
3. **混合检索**：向量（语义）+ 关键词（字面）+ 结构化过滤 + RRF 重排。
4. **失败降级**：向量检索不可用时自动回退关键词/全库，AI 导购整体不中断。
5. **上下文控制**：商品目录按「分类 Top3」压缩进 system prompt，历史只保留最近 20 条。
6. **工具轮次上限**（3 轮）：防止 LLM 无限调用工具。

## 四、索引构建（启动时）

```
应用启动 → ProductIndexBootstrap
  → 加载全部商品(429件)
  → ProductTextBuilder 拼文本(名称+分类+描述+语义标签)
  → 百炼批量嵌入(1024维)
  → Qdrant 建集合 + upsert
```

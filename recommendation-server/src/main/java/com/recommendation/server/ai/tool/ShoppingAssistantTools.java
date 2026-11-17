package com.recommendation.server.ai.tool;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.recommendation.pojo.vo.ProductVO;
import com.recommendation.pojo.vo.RecommendationVO;
import com.recommendation.server.ai.rag.HybridProductSearchService;
import com.recommendation.server.service.ProductService;
import com.recommendation.server.service.RecommendationService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

// 三个工具函数整合在一个 Bean 中，双路径设计：
// 1. @Tool 注解 → ToolSpecifications.toolSpecificationsFrom() 自动生成 LLM 工具规范
// 2. execute() 手动调度 → ChatServiceImpl 的工具循环按 functionName 路由，
//    收集商品数据用于 SSE product 事件（这是 AiServices 做不到的）
@Slf4j
@Component
@RequiredArgsConstructor
public class ShoppingAssistantTools {

    private final ProductService productService;
    private final RecommendationService recommendationService;
    private final UserContextHolder userContext;
    private final HybridProductSearchService hybridProductSearchService;

    @Tool(name = "search_products",
          value = "根据关键词、分类、价格范围搜索商品")
    public String searchProducts(
            @P("搜索关键词") String keyword,
            @P("商品分类名称") String categoryName,
            @P("最低价格") Double minPrice,
            @P("最高价格") Double maxPrice,
            @P("返回数量限制，默认3") Integer limit) {
        int n = Math.min(limit != null ? limit : 3, 3);
        return doSearchProducts(keyword, categoryName, minPrice, maxPrice, n);
    }

    @Tool(name = "get_product_detail",
          value = "获取特定商品的详细信息，包括完整描述、库存、评分等")
    public String getProductDetail(
            @P("商品ID") Long productId) {
        return doGetProductDetail(productId);
    }

    @Tool(name = "get_recommendations",
          value = "获取当前用户的个性化商品推荐，基于协同过滤算法")
    public String getRecommendations(
            @P("推荐数量，默认3") Integer topN) {
        return doGetRecommendations(Math.min(topN != null ? topN : 3, 3));
    }

    // 从 @Tool 注解自动提取工具规范，无需手写 JSON Schema
    public List<ToolSpecification> getSpecifications() {
        return ToolSpecifications.toolSpecificationsFrom(this);
    }

    // 手动调度入口：ChatServiceImpl 的工具循环调用此方法而非 @Tool 方法，
    // 因为 LLM 返回的 arguments 是 JSON 字符串，需要先解析再路由
    public String execute(String functionName, String jsonArguments) {
        try {
            JSONObject args;
            try {
                args = JSON.parseObject(
                        jsonArguments != null && !jsonArguments.isEmpty()
                                ? jsonArguments : "{}");
            } catch (Exception e) {
                // DeepSeek 部分场景用 DSML 标记而非标准 JSON，做宽松提取兜底
                log.warn("工具参数非 JSON，启用 DSML 宽松提取: functionName={}, raw={}",
                        functionName, truncate(jsonArguments));
                args = extractArgsFromDsml(jsonArguments);
                if (args.isEmpty()) {
                    return "{\"error\": \"工具参数无法解析\"}";
                }
            }
            return switch (functionName) {
                case "search_products" -> doSearchProducts(
                        args.getString("keyword"),
                        args.getString("categoryName"),
                        args.getDouble("minPrice"),
                        args.getDouble("maxPrice"),
                        Math.min(args.getIntValue("limit", 3), 3));
                case "get_product_detail" -> doGetProductDetail(
                        args.getLong("productId"));
                case "get_recommendations" -> doGetRecommendations(
                        Math.min(args.getIntValue("topN", 3), 3));
                default -> "{\"error\": \"Unknown function: " + functionName + "\"}";
            };
        } catch (Exception e) {
            // 兜底：任何异常都不让工具调用导致整个对话崩溃
            log.error("工具执行失败 functionName={}", functionName, e);
            return "{\"error\": \"工具执行失败: " + e.getMessage() + "\"}";
        }
    }

    private String truncate(String s) {
        if (s == null) {
            return "null";
        }
        return s.length() <= 200 ? s : s.substring(0, 200) + "...";
    }

    /**
     * 从 DSML 标记中宽松提取工具参数。
     * 格式示例：&lt;｜DSML｜parameter name="keyword" string="true"&gt;复古墨镜&lt;/｜DSML｜parameter&gt;
     */
    JSONObject extractArgsFromDsml(String raw) {
        JSONObject args = new JSONObject();
        if (raw == null || raw.isEmpty()) {
            return args;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("name=\"([^\"]+)\"[^>]*>([^<]*)").matcher(raw);
        while (m.find()) {
            putArg(args, m.group(1).trim(), m.group(2).trim());
        }
        return args;
    }

    private void putArg(JSONObject args, String name, String value) {
        switch (name) {
            case "keyword", "categoryName" -> args.put(name, value);
            case "minPrice", "maxPrice" -> {
                try {
                    args.put(name, Double.parseDouble(value));
                } catch (NumberFormatException ignored) {
                }
            }
            case "limit", "topN" -> {
                try {
                    args.put(name, Integer.parseInt(value.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
            case "productId" -> {
                try {
                    args.put(name, Long.parseLong(value.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
            default -> {
            }
        }
    }

    // 搜索降级链：keyword 精准搜索 → 分类宽泛搜索 → 全库按销量排序
    // 解决 AI 猜品牌名（如"三只松鼠"）但库中无此品牌导致空结果的问题
    private String doSearchProducts(String keyword, String categoryName,
                                     Double minPrice, Double maxPrice, int limit) {
        // 混合检索：向量语义召回 + 关键词召回 + 结构化过滤 + RRF 重排，无结果自动降级为热门兜底
        List<ProductVO> results = hybridProductSearchService.search(
                keyword, categoryName, minPrice, maxPrice, limit);

        return JSON.toJSONString(results.stream()
                .map(p -> {
                    JSONObject obj = new JSONObject();
                    obj.put("productId", p.getId());
                    obj.put("name", p.getName());
                    obj.put("categoryName", p.getCategoryName() != null ? p.getCategoryName() : "");
                    obj.put("price", p.getPrice());
                    obj.put("sales", p.getSales());
                    obj.put("description", p.getDescription() != null ? p.getDescription() : "");
                    return obj;
                })
                .collect(Collectors.toList()));
    }

    // 用 JSONObject 逐字段构建而非 Map.of()：ProductVO 的 averageRating、ratingCount
    // 等字段可能为 null，Map.of() 不允许 null 值
    private String doGetProductDetail(Long productId) {
        ProductVO product = productService.getProductById(productId);

        JSONObject obj = new JSONObject();
        obj.put("productId", product.getId());
        obj.put("name", product.getName());
        obj.put("categoryName", product.getCategoryName() != null ? product.getCategoryName() : "");
        obj.put("price", product.getPrice());
        obj.put("stock", product.getStock());
        obj.put("sales", product.getSales());
        obj.put("averageRating", product.getAverageRating());
        obj.put("ratingCount", product.getRatingCount());
        obj.put("description", product.getDescription() != null ? product.getDescription() : "");
        return JSON.toJSONString(obj);
    }

    private String doGetRecommendations(int topN) {
        // 从 ThreadLocal 获取当前用户，兜底为 1L（匿名用户/测试场景）
        Long userId = userContext.get();
        if (userId == null) userId = 1L;

        List<RecommendationVO> results = recommendationService.recommend(userId, topN);

        return JSON.toJSONString(results.stream()
                .map(r -> {
                    JSONObject obj = new JSONObject();
                    obj.put("productId", r.getProductId());
                    obj.put("name", r.getProductName());
                    obj.put("price", r.getPrice());
                    obj.put("score", r.getScore());
                    obj.put("reason", r.getReason() != null ? r.getReason() : "");
                    return obj;
                })
                .collect(Collectors.toList()));
    }
}

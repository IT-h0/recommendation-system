package com.recommendation.server.ai;

import com.recommendation.pojo.entity.Category;
import com.recommendation.pojo.entity.Product;
import com.recommendation.server.mapper.CategoryMapper;
import com.recommendation.server.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductContextBuilder {

    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;

    // 商品目录缓存 10 分钟：避免每次对话都查全库，商品变更通过 CategoryService/ProductService
    // 的 evictCache 方法主动失效
    private String cachedCatalog;
    private long lastBuildTime;
    private static final long CACHE_TTL = 600_000;
    // 每个分类只展示销量 Top N，控制 context 体积，减少 token 消耗
    private static final int MAX_PRODUCTS_PER_CATEGORY = 3;

    public String buildSystemPrompt() {
        return """
                你是一个专业的电商导购助手"小荐"。你的职责是:
                1. 理解用户的购物需求,从商品库中精准推荐
                2. 解释每个推荐的理由,贴合用户的需求
                3. 语气友好、专业、有耐心,用中文回复

                你可以使用以下工具:
                - search_products: 根据关键词、分类、价格范围搜索商品
                - get_recommendations: 获取当前用户的个性化推荐
                - get_product_detail: 获取某个商品的详细信息

                重要规则:
                - 每次最多推荐3件商品
                - 推荐时必须包含: 商品名称、价格、推荐理由
                - 如果找不到完美匹配,诚实说明并建议相近替代品
                - 不要在回复中编造不存在的商品
                - 回复简洁,重点突出
                - 当用户需求比较模糊时(如"送礼物"、"买点吃的"),先用宽泛的分类名搜索了解库里有
                  什么商品,再基于实际搜索结果推荐,不要凭空猜测品牌名或商品名
                - 当用户追问上一轮推荐商品的详情/参数时(如"说说XX的参数"),直接用 get_product_detail
                  并传上下文里标注的对应 productId,不要再重新搜索推荐

                当前商品库概览(按分类):
                """ + buildCatalogSummary();
    }

    public String buildCatalogSummary() {
        long now = System.currentTimeMillis();
        if (cachedCatalog != null && (now - lastBuildTime) <= CACHE_TTL) {
            return cachedCatalog;
        }

        List<Product> products = productMapper.selectAll();
        Map<Long, Category> categoryMap = categoryMapper.selectAll().stream()
                .collect(Collectors.toMap(Category::getId, c -> c, (a, b) -> a));

        // 按分类分组，LinkedHashMap 保持分类插入顺序
        Map<String, List<Product>> grouped = new LinkedHashMap<>();
        for (Product p : products) {
            Category cat = categoryMap.get(p.getCategoryId());
            String catName = cat != null ? cat.getName() : "其他";
            // 二级分类展示为 "父分类>子分类"，如 "手机数码>智能手机"
            if (cat != null && cat.getParentId() != null && cat.getParentId() != 0) {
                Category parent = categoryMap.get(cat.getParentId());
                if (parent != null) {
                    catName = parent.getName() + ">" + cat.getName();
                }
            }
            grouped.computeIfAbsent(catName, k -> new ArrayList<>()).add(p);
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<Product>> entry : grouped.entrySet()) {
            sb.append("- ").append(entry.getKey())
              .append("(").append(entry.getValue().size()).append("件): ");

            String items = entry.getValue().stream()
                    .sorted((a, b) -> Integer.compare(
                            b.getSales() != null ? b.getSales() : 0,
                            a.getSales() != null ? a.getSales() : 0))
                    .limit(MAX_PRODUCTS_PER_CATEGORY)
                    .map(p -> p.getName() + "(¥" + p.getPrice() + ")")
                    .collect(Collectors.joining(", "));

            if (entry.getValue().size() > MAX_PRODUCTS_PER_CATEGORY) {
                items += " ...等";
            }
            sb.append(items).append("\n");
        }

        cachedCatalog = sb.toString();
        lastBuildTime = now;
        return cachedCatalog;
    }
}

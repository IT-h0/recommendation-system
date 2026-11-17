package com.recommendation.server.ai.eval;

import java.util.List;
import java.util.Set;

/**
 * 共享的 Golden 评测集：语义查询 -> 相关分类 ID。
 * 检索评测与生成评测共用同一份数据，保证指标可比。
 */
public final class GoldenDataset {

    private GoldenDataset() {
    }

    public static List<GoldenQuery> standard() {
        return List.of(
                new GoldenQuery("我想买一部手机", Set.of(11L)),
                new GoldenQuery("办公用的轻薄笔记本", Set.of(12L)),
                new GoldenQuery("随身携带的平板电脑", Set.of(13L)),
                new GoldenQuery("戴在手上的智能手表", Set.of(14L)),
                new GoldenQuery("客厅里玩游戏的主机", Set.of(15L)),
                new GoldenQuery("拍照用的相机", Set.of(16L)),
                new GoldenQuery("夏天穿的连衣裙", Set.of(19L)),
                new GoldenQuery("跑步穿的鞋子", Set.of(20L)),
                new GoldenQuery("追剧时吃的小零食", Set.of(23L)),
                new GoldenQuery("提神喝的咖啡", Set.of(27L)),
                new GoldenQuery("睡觉盖的被子床单", Set.of(28L)),
                new GoldenQuery("做饭用的锅具", Set.of(29L)),
                new GoldenQuery("在家锻炼的健身器材", Set.of(37L)),
                new GoldenQuery("爬山露营的户外装备", Set.of(38L)),
                new GoldenQuery("抹在脸上的护肤精华", Set.of(41L)),
                new GoldenQuery("涂在嘴上的口红", Set.of(42L)),
                new GoldenQuery("宝宝用的纸尿裤", Set.of(45L)),
                new GoldenQuery("给小孩子买的玩具", Set.of(46L)),
                new GoldenQuery("车上的行车记录仪", Set.of(48L)),
                new GoldenQuery("汽车保养用的机油", Set.of(50L)),
                new GoldenQuery("给猫买的用品", Set.of(51L)),
                new GoldenQuery("给狗买的用品", Set.of(52L)),
                new GoldenQuery("宠物吃的食物", Set.of(53L))
        );
    }
}

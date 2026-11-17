package com.recommendation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 推荐系统启动类
 */
@SpringBootApplication
public class RecommendationApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecommendationApplication.class, args);
        System.out.println("推荐系统启动成功!");
        System.out.println("API文档地址: http://localhost:8080/doc.html");
    }
}

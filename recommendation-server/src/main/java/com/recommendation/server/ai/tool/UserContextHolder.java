package com.recommendation.server.ai.tool;

import org.springframework.stereotype.Component;

// ThreadLocal 而非 @RequestScope：chat() 在 CompletableFuture 异步线程中执行，
// 不在请求线程内，@RequestScope 无法生效。chat() 开头 set，finally 中 clear
@Component
public class UserContextHolder {

    private static final ThreadLocal<Long> CURRENT_USER = new ThreadLocal<>();

    public void set(Long userId) {
        CURRENT_USER.set(userId);
    }

    public Long get() {
        return CURRENT_USER.get();
    }

    public void clear() {
        CURRENT_USER.remove();
    }
}

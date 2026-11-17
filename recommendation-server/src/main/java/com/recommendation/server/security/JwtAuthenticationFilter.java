package com.recommendation.server.security;

import com.recommendation.common.util.JwtUtil;
import com.recommendation.common.util.RedisUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT认证过滤器
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        // 登录和注册接口跳过JWT验证
        if (requestURI.equals("/api/user/login") || requestURI.equals("/api/user/register")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 从请求头中获取Token
        String authHeader = request.getHeader("Authorization");

        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // 验证Token
            if (jwtUtil.validateToken(token)) {
                Long userId = jwtUtil.getUserIdFromToken(token);

                // 检查Redis中是否存在该Token
                String redisKey = "rec:user:" + userId;
                if (redisUtil.hasKey(redisKey)) {
                    // 创建认证对象
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userId,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                            );

                    // 设置请求属性,供Controller使用
                    request.setAttribute("userId", userId);

                    // 设置认证信息
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 返回false确保SSE等异步请求完成时的async dispatch也会经过此过滤器重新认证，
     * 避免SecurityContext失效后AuthorizationFilter拦截抛出AccessDeniedException
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }
}

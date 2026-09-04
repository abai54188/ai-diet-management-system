package com.diet.interceptor;

import com.diet.common.Result;
import com.diet.common.ResultCode;
import com.diet.common.UserContext;
import com.diet.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 认证拦截器
 * 1. 校验请求头中的令牌，未通过则返回 401
 * 2. 校验通过后将用户信息写入 UserContext 供业务层使用
 * 3. 对 /api/admin/** 开头的接口额外校验管理员角色
 *
 * @author diet
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    private final ObjectMapper objectMapper;

    public JwtInterceptor(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");
        // 令牌缺失或格式错误
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return writeError(response, ResultCode.UNAUTHORIZED);
        }
        try {
            // 解析并校验令牌(签名、有效期)
            Claims claims = jwtUtil.parseToken(authHeader.substring(7));
            Long userId = claims.get("userId", Long.class);
            String username = claims.getSubject();
            String role = claims.get("role", String.class);
            // 管理员接口的权限校验
            if (request.getRequestURI().startsWith("/api/admin") && !"ADMIN".equals(role)) {
                return writeError(response, ResultCode.FORBIDDEN);
            }
            // 将用户信息写入当前线程上下文
            UserContext.set(new UserContext.UserInfo(userId, username, role));
            return true;
        } catch (Exception e) {
            return writeError(response, ResultCode.UNAUTHORIZED);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束后清理 ThreadLocal，防止内存泄漏
        UserContext.clear();
    }

    /**
     * 拦截器中直接输出统一 JSON 结果(HTTP 状态码固定 200，业务码区分)
     */
    private boolean writeError(HttpServletResponse response, ResultCode resultCode) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.fail(resultCode)));
        return false;
    }
}
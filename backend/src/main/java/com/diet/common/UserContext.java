package com.diet.common;

/**
 * 当前登录用户上下文
 * JwtInterceptor 校验通过后写入，业务代码中通过静态方法获取当前用户信息
 *
 * @author diet
 */
public class UserContext {

    private static final ThreadLocal<UserInfo> HOLDER = new ThreadLocal<>();

    /**
     * 设置当前登录用户信息(仅拦截器调用)
     */
    public static void set(UserInfo userInfo) {
        HOLDER.set(userInfo);
    }

    /**
     * 获取当前登录用户信息
     */
    public static UserInfo get() {
        return HOLDER.get();
    }

    /**
     * 清理线程变量(必须调用，防止内存泄漏)
     */
    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 登录用户信息载体
     */
    public static class UserInfo {

        /** 用户ID */
        private Long userId;

        /** 用户名 */
        private String username;

        /** 角色: USER-普通用户, ADMIN-管理员 */
        private String role;

        public UserInfo(Long userId, String username, String role) {
            this.userId = userId;
            this.username = username;
            this.role = role;
        }

        public Long getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getRole() {
            return role;
        }
    }
}
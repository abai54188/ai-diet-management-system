package com.diet.common;

/**
 * 全局统一返回状态码枚举
 *
 * @author diet
 */
public enum ResultCode {

    /** 操作成功 */
    SUCCESS(200, "操作成功"),

    /** 参数校验失败 */
    PARAM_ERROR(400, "请求参数错误"),

    /** 未登录或令牌失效 */
    UNAUTHORIZED(401, "未登录或登录已过期，请重新登录"),

    /** 无访问权限 */
    FORBIDDEN(403, "无权限访问该资源"),

    /** 业务处理失败 */
    BUSINESS_ERROR(600, "业务处理失败"),

    /** 系统内部错误 */
    SYSTEM_ERROR(500, "系统内部错误，请联系管理员");

    private final Integer code;

    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
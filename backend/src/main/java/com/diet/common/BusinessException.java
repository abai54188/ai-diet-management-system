package com.diet.common;

/**
 * 自定义业务异常
 * 仅用于可预期的业务错误(如用户名已存在、密码错误等)
 *
 * @author diet
 */
public class BusinessException extends RuntimeException {

    /** 业务错误码 */
    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.BUSINESS_ERROR.getCode();
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
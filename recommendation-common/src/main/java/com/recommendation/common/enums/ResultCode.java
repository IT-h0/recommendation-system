package com.recommendation.common.enums;

import lombok.Getter;

/**
 * 响应状态码
 */
@Getter
public enum ResultCode {

    /** 成功 */
    SUCCESS(200, "操作成功"),

    /** 参数错误 */
    PARAM_ERROR(400, "参数错误"),

    /** 未认证 */
    UNAUTHORIZED(401, "未认证"),

    /** 无权限 */
    FORBIDDEN(403, "无权限"),

    /** 资源不存在 */
    NOT_FOUND(404, "资源不存在"),

    /** 请求方法不支持 */
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    /** 系统内部错误 */
    INTERNAL_SERVER_ERROR(500, "系统内部错误"),

    /** 业务异常 */
    BUSINESS_ERROR(1000, "业务异常"),

    /** 用户名或密码错误 */
    USERNAME_PASSWORD_ERROR(1001, "用户名或密码错误"),

    /** 用户已存在 */
    USER_EXISTS(1002, "用户已存在"),

    /** 用户不存在 */
    USER_NOT_EXISTS(1003, "用户不存在"),

    /** Token无效或已过期 */
    TOKEN_INVALID(1004, "Token无效或已过期"),

    /** 商品不存在 */
    PRODUCT_NOT_EXISTS(2001, "商品不存在"),

    /** 商品库存不足 */
    PRODUCT_STOCK_INSUFFICIENT(2002, "商品库存不足"),

    /** 评分失败 */
    RATING_FAILED(3001, "评分失败"),

    /** 推荐算法执行失败 */
    RECOMMENDATION_FAILED(4001, "推荐算法执行失败");

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

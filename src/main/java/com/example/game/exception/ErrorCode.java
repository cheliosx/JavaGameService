package com.example.game.exception;

import com.iohao.game.action.skeleton.core.exception.MsgExceptionInfo;

/**
 * 业务错误码。
 *
 * <p>实现 {@link MsgExceptionInfo} 后，即可用 {@code ErrorCode.xxx.assertTrue(条件)} 抛出业务异常，
 * 框架会自动把 code 塞进响应的 responseStatus、把 msg 塞进 validMsg，客户端无需解析字符串。
 */
public enum ErrorCode implements MsgExceptionInfo {

    PARAM_EMPTY(1001, "用户名或密码不能为空"),
    USERNAME_OR_PASSWORD_ERROR(1002, "用户名或密码错误"),
    NOT_LOGIN(1003, "未登录或登录态已失效"),
    TOKEN_INVALID(1004, "登录凭证无效"),
    BIND_USER_FAILED(1005, "登录态绑定失败，请重试"),
    ALREADY_LOGIN(1006, "该连接已登录，请先退出登录"),
    ;

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public int getCode() {
        return this.code;
    }

    @Override
    public String getMsg() {
        return this.msg;
    }
}

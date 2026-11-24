package com.example.game.entity;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

/**
 * 登录请求体（客户端 -> 服务端）。
 *
 * <p>{@code @ProtobufClass} 是 ioGame 采用的 jprotobuf 简化写法：
 * 字段全部 public，无需手写 .proto 与生成代码，序列化性能与原生 protobuf 一致。
 */
@ProtobufClass
public class LoginVerifyMessage {

    /** 用户名 */
    public String username;

    /** 明文密码，仅用于本次校验，绝不落库 */
    public String password;

    public LoginVerifyMessage() {
    }

    public LoginVerifyMessage(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public String toString() {
        // 注意：日志里不要打印 password 明文
        return "LoginVerifyMessage{username='" + username + "'}";
    }
}

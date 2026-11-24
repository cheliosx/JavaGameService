package com.example.game.entity;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

/**
 * 登录响应体（服务端 -> 客户端）。
 */
@ProtobufClass
public class LoginMessage {

    /** 用户 id，后续所有协议都以它为准 */
    public long userId;

    /** 昵称 */
    public String nickname;

    /** 登录凭证：换连接 / 重连时使用 */
    public String token;

    /** token 过期时间（毫秒时间戳） */
    public long expireAt;

    @Override
    public String toString() {
        return "LoginMessage{userId=" + userId
                + ", nickname='" + nickname + '\''
                + ", expireAt=" + expireAt + '}';
    }
}

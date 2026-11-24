package com.example.game.entity;

import com.baidu.bjf.remoting.protobuf.annotation.ProtobufClass;

/**
 * 当前登录用户信息（需要已登录）。
 */
@ProtobufClass
public class UserInfoMessage {

    public long userId;

    public String username;

    public String nickname;

    /** 登录时间（毫秒时间戳） */
    public long loginAt;

    @Override
    public String toString() {
        return "UserInfoMessage{userId=" + userId
                + ", username='" + username + '\''
                + ", nickname='" + nickname + '\''
                + ", loginAt=" + loginAt + '}';
    }
}

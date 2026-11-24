package com.example.game.cmd;

/**
 * 登录相关路由常量。
 *
 * <p>ioGame 的路由由「主路由 cmd」+「子路由 subCmd」两段组成，
 * 对外表现为 {@code cmd-subCmd}，例如登录是 {@code 1-0}。
 * 客户端与服务端共用同一份常量，避免魔法数字。
 */
public interface LoginCmd {

    /** 主路由：登录域 */
    int cmd = 1;

    /** 登录：1-0 */
    int login = 0;

    /** 当前登录用户信息：1-1 */
    int me = 1;

    /** 退出登录：1-2 */
    int logout = 2;
}

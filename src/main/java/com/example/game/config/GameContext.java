package com.example.game.config;

import com.example.game.service.LoginSessionService;
import com.example.game.service.UserService;

/**
 * 极简服务容器。
 *
 * <p>脚手架阶段不引入 Spring 容器：ioGame 默认由框架反射实例化 Action，
 * 这里用静态持有者提供单例，结构清晰且零依赖。
 * 若后续要接 Spring Boot，只需把这两个方法改成从 {@code ApplicationContext} 取 Bean。
 */
public final class GameContext {

    private static volatile UserService userService = new UserService();
    private static volatile LoginSessionService loginSessionService = new LoginSessionService();

    private GameContext() {
    }

    public static UserService userService() {
        return userService;
    }

    public static LoginSessionService loginSessionService() {
        return loginSessionService;
    }

    /**
     * 重置为全新实例。仅用于测试，保证用例之间互不影响。
     */
    public static void reset() {
        userService = new UserService();
        loginSessionService = new LoginSessionService();
    }
}

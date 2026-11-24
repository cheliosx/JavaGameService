package com.example.game.action;

import com.example.game.cmd.LoginCmd;
import com.example.game.config.GameContext;
import com.example.game.entity.LoginMessage;
import com.example.game.entity.LoginVerifyMessage;
import com.example.game.entity.UserInfoMessage;
import com.example.game.exception.ErrorCode;
import com.example.game.support.GameTestClient;
import com.example.game.support.TestServer;
import com.iohao.game.external.core.message.ExternalMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 登录接口端到端测试。
 *
 * <p>真实链路：JDK WebSocket -> 对外服(Netty) -> 游戏网关 -> 逻辑服 -> Action。
 * 走的是完整的协议序列化与路由，不是「直接 new Action 调方法」的伪测试。
 */
@DisplayName("登录接口 端到端")
class LoginActionIntegrationTest {

    private static int port;

    private GameTestClient client;

    @BeforeAll
    static void startServer() {
        port = TestServer.start();
    }

    @BeforeEach
    void setUp() {
        GameContext.reset();
        client = GameTestClient.connect(port);
    }

    @AfterEach
    void tearDown() {
        client.close();
    }

    @Test
    @DisplayName("正确账号密码登录成功，返回 userId / 昵称 / token")
    void loginSuccess() {
        LoginMessage result = client.request(
                LoginCmd.cmd, LoginCmd.login,
                new LoginVerifyMessage("admin", "admin123"),
                LoginMessage.class);

        assertNotNull(result);
        assertEquals(1L, result.userId);
        assertEquals("管理员", result.nickname);
        assertNotNull(result.token);
        assertTrue(result.token.length() >= 32, "token 应为足够长的随机串");
        assertTrue(result.expireAt > System.currentTimeMillis(), "过期时间应在将来");
        assertTrue(GameContext.loginSessionService().verify(result.token).isPresent());
    }

    @Test
    @DisplayName("密码错误返回业务错误码 1002")
    void loginWithWrongPassword() {
        ExternalMessage response = client.request(
                LoginCmd.cmd, LoginCmd.login,
                new LoginVerifyMessage("admin", "wrong-password"));

        // 业务失败只回报错误码，不返回业务数据；文案由客户端按码表本地化
        assertEquals(ErrorCode.USERNAME_OR_PASSWORD_ERROR.getCode(), response.getResponseStatus());
        assertNull(response.getData(), "失败响应不应携带业务数据");
    }

    @Test
    @DisplayName("用户不存在也返回 1002，不暴露账号是否存在")
    void loginWithUnknownUser() {
        ExternalMessage response = client.request(
                LoginCmd.cmd, LoginCmd.login,
                new LoginVerifyMessage("no-such-user", "any"));

        assertEquals(ErrorCode.USERNAME_OR_PASSWORD_ERROR.getCode(), response.getResponseStatus());
    }

    @Test
    @DisplayName("用户名或密码为空返回 1001")
    void loginWithEmptyParam() {
        ExternalMessage blankUsername = client.request(
                LoginCmd.cmd, LoginCmd.login,
                new LoginVerifyMessage("", "admin123"));
        assertEquals(ErrorCode.PARAM_EMPTY.getCode(), blankUsername.getResponseStatus());

        ExternalMessage nullPassword = client.request(
                LoginCmd.cmd, LoginCmd.login,
                new LoginVerifyMessage("admin", null));
        assertEquals(ErrorCode.PARAM_EMPTY.getCode(), nullPassword.getResponseStatus());
    }

    @Test
    @DisplayName("未登录直接查当前用户返回 1003")
    void meWithoutLogin() {
        ExternalMessage response = client.request(LoginCmd.cmd, LoginCmd.me, null);

        assertEquals(ErrorCode.NOT_LOGIN.getCode(), response.getResponseStatus());
    }

    @Test
    @DisplayName("登录后同一连接无需再传 userId 即可查到当前用户（验证连接绑定生效）")
    void meAfterLogin() {
        LoginMessage login = client.request(
                LoginCmd.cmd, LoginCmd.login,
                new LoginVerifyMessage("player", "123456"),
                LoginMessage.class);
        assertEquals(2L, login.userId);

        // 注意：这里没有传任何参数，userId 完全来自连接绑定
        UserInfoMessage me = client.request(
                LoginCmd.cmd, LoginCmd.me, null, UserInfoMessage.class);

        assertNotNull(me);
        assertEquals(2L, me.userId);
        assertEquals("player", me.username);
        assertEquals("玩家一号", me.nickname);
    }

    @Test
    @DisplayName("退出登录是单向协议：不回包，但 token 已作废")
    void logout() {
        LoginMessage login = client.request(
                LoginCmd.cmd, LoginCmd.login,
                new LoginVerifyMessage("admin", "admin123"),
                LoginMessage.class);

        client.sendOneway(LoginCmd.cmd, LoginCmd.logout, null);

        assertFalse(client.receivedAny(Duration.ofMillis(300)), "void 方法不应产生响应包");
        assertTrue(GameContext.loginSessionService().verify(login.token).isEmpty(),
                "退出后 token 应失效");
    }

    @Test
    @DisplayName("同一连接重复登录被拒绝（返回 1006，而不是框架底层的 1005）")
    void loginTwiceOnSameConnectionIsRejected() {
        LoginMessage first = client.request(
                LoginCmd.cmd, LoginCmd.login,
                new LoginVerifyMessage("admin", "admin123"),
                LoginMessage.class);
        assertEquals(1L, first.userId);

        ExternalMessage second = client.request(
                LoginCmd.cmd, LoginCmd.login,
                new LoginVerifyMessage("admin", "admin123"));

        assertEquals(ErrorCode.ALREADY_LOGIN.getCode(), second.getResponseStatus());
    }
}

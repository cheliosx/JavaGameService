package com.example.game.action;

import com.example.game.cmd.LoginCmd;
import com.example.game.config.GameContext;
import com.example.game.entity.LoginMessage;
import com.example.game.entity.LoginVerifyMessage;
import com.example.game.entity.UserInfoMessage;
import com.example.game.exception.ErrorCode;
import com.example.game.service.LoginSessionService;
import com.example.game.service.UserService;
import com.iohao.game.action.skeleton.annotation.ActionController;
import com.iohao.game.action.skeleton.annotation.ActionMethod;
import com.iohao.game.action.skeleton.core.exception.MsgException;
import com.iohao.game.action.skeleton.core.flow.FlowContext;
import com.iohao.game.action.skeleton.protocol.login.SettingUserIdResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 登录控制器。
 *
 * <p>路由：{@code cmd=1}，子路由见 {@link LoginCmd}。
 *
 * <p>三个方法演示了脚手架里最重要的三件事：
 * <ol>
 *     <li>{@code login}：校验身份 + 签发 token + <b>把 userId 绑定到当前连接</b></li>
 *     <li>{@code me}：从连接上取 userId，验证绑定确实生效</li>
 *     <li>{@code logout}：解绑并吊销 token</li>
 * </ol>
 */
@ActionController(LoginCmd.cmd)
public class LoginAction {

    private static final Logger log = LoggerFactory.getLogger(LoginAction.class);

    /**
     * 登录：1-0
     *
     * <p>{@link FlowContext} 由框架注入，用来读写「当前这条连接」的上下文。
     * 它是本方法能把登录态落到连接上的唯一途径。
     */
    @ActionMethod(LoginCmd.login)
    public LoginMessage login(LoginVerifyMessage message, FlowContext flowContext) {
        ErrorCode.PARAM_EMPTY.assertNonNull(message);
        ErrorCode.PARAM_EMPTY.assertTrue(notBlank(message.username) && notBlank(message.password));

        // ioGame 的连接一旦绑定 userId 就不可再绑定（重复绑定会被框架拒绝并回 1005），
        // 因此必须自己拦截重复登录，给客户端一个明确的错误码
        ErrorCode.ALREADY_LOGIN.assertTrue(currentUserId(flowContext) <= 0);

        UserService userService = GameContext.userService();
        UserService.User user = userService.verify(message.username, message.password)
                .orElseThrow(() -> new MsgException(ErrorCode.USERNAME_OR_PASSWORD_ERROR));

        LoginSessionService.Session session = GameContext.loginSessionService().create(user.userId());

        // 把 userId 绑定到当前连接：此后该连接上的请求，框架会自动带上 userId。
        // setUserId(long) 已废弃（该动作语义是「绑定」而非「设置」，且绑定后不可更改），
        // 改用 bindingUserIdAndGetResult：失败时能拿到具体原因，而不是只有一个 false。
        SettingUserIdResult bindResult = flowContext.bindingUserIdAndGetResult(user.userId());
        if (!bindResult.success()) {
            // 走到这里说明前置的 ALREADY_LOGIN 没拦住，属于异常场景，需要完整堆栈定位
            log.warn("登录态绑定失败 userId={} username={}", user.userId(), user.username(), bindResult.exception());
        }
        ErrorCode.BIND_USER_FAILED.assertTrue(bindResult.success());

        log.info("用户登录成功 userId={} username={}", user.userId(), user.username());

        LoginMessage result = new LoginMessage();
        result.userId = user.userId();
        result.nickname = user.nickname();
        result.token = session.token();
        result.expireAt = session.expireAt();
        return result;
    }

    /**
     * 当前登录用户：1-1
     *
     * <p>无需任何参数 —— userId 完全来自连接绑定，这也是验证登录是否真正生效的手段。
     */
    @ActionMethod(LoginCmd.me)
    public UserInfoMessage me(FlowContext flowContext) {
        long userId = currentUserId(flowContext);
        ErrorCode.NOT_LOGIN.assertTrue(userId > 0);

        UserService.User user = GameContext.userService().findById(userId)
                .orElseThrow(() -> new MsgException(ErrorCode.NOT_LOGIN));

        UserInfoMessage result = new UserInfoMessage();
        result.userId = user.userId();
        result.username = user.username();
        result.nickname = user.nickname();
        result.loginAt = System.currentTimeMillis();
        return result;
    }

    /**
     * 退出登录：1-2
     *
     * <p><b>单向协议</b>：返回 {@code void} 的 action 不会产生响应包（客户端不等待回执）。
     * 登出属于「尽力而为」操作，客户端发完即可主动断开重连。
     *
     * <p>注意：这里<b>不调用</b> {@code bindingUserId(0)} 试图解绑 —— ioGame 的连接级 userId
     * 绑定是单向不可逆的（既不能改绑，也不能置 0）。真正的「退出」靠客户端断连完成，
     * 断连后新连接自然没有 userId。服务端这边的职责是让 token 作废。
     *
     * <p>若要做「服务端强制踢人」，需要借助用户会话管理主动关闭对应连接，属于后续扩展点。
     */
    @ActionMethod(LoginCmd.logout)
    public void logout(FlowContext flowContext) {
        long userId = currentUserId(flowContext);
        ErrorCode.NOT_LOGIN.assertTrue(userId > 0);

        GameContext.loginSessionService().revokeByUserId(userId);
        log.info("用户退出登录 userId={}", userId);
    }

    /**
     * 从当前连接的请求头里取 userId。
     *
     * <p>未登录时框架给的是 0，因此业务侧统一用 {@code userId > 0} 判断登录态。
     */
    private static long currentUserId(FlowContext flowContext) {
        if (flowContext == null
                || flowContext.getRequest() == null
                || flowContext.getRequest().getHeadMetadata() == null) {
            return 0L;
        }
        return flowContext.getRequest().getHeadMetadata().getUserId();
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}

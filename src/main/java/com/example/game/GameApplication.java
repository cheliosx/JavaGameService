package com.example.game;

import com.example.game.cmd.LoginCmd;
import com.example.game.logic.GameLogicServer;
import com.iohao.game.bolt.broker.core.common.IoGameGlobalConfig;
import com.iohao.game.external.core.config.ExternalGlobalConfig;
import com.iohao.game.external.core.netty.simple.NettySimpleHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 服务器启动入口。
 *
 * <p>ioGame 的「一体化」启动：一个 JVM 里同时拉起
 * <b>对外服（Netty，客户端连这里）</b> + <b>游戏网关</b> + <b>逻辑服</b>。
 *
 * <p>默认端口：对外服 10100，网关 10200。
 * 启动参数可覆盖对外服端口：{@code java -jar app.jar 10100}
 */
public final class GameApplication {

    private static final Logger log = LoggerFactory.getLogger(GameApplication.class);

    public static final int DEFAULT_EXTERNAL_PORT = 10100;
    public static final int DEFAULT_BROKER_PORT = 10200;

    private GameApplication() {
    }

    public static void main(String[] args) {
        int externalPort = DEFAULT_EXTERNAL_PORT;
        if (args.length > 0) {
            externalPort = Integer.parseInt(args[0]);
        }
        start(externalPort, DEFAULT_BROKER_PORT);
    }

    /**
     * 启动服务器。
     *
     * @param externalPort 对外服端口（客户端 WebSocket 连这个）
     * @param brokerPort   游戏网关端口（逻辑服连这个，仅内网）
     */
    public static void start(int externalPort, int brokerPort) {
        configure(externalPort, brokerPort);

        log.info("""
                
                ============================================================
                  游戏服务器启动中
                  对外服 (WebSocket) : ws://127.0.0.1:{}
                  游戏网关            : 127.0.0.1:{}
                ============================================================
                """, externalPort, brokerPort);

        NettySimpleHelper.run(externalPort, List.of(new GameLogicServer()));
    }

    /**
     * 启动前的全局配置。测试也会复用，保证两侧配置完全一致。
     */
    public static void configure(int externalPort, int brokerPort) {
        IoGameGlobalConfig.brokerPort = brokerPort;

        // 登录接口必须免鉴权：此时连接上还没有 userId，若要求鉴权则永远无法登录
        if (ExternalGlobalConfig.accessAuthenticationHook != null) {
            ExternalGlobalConfig.accessAuthenticationHook.addIgnoreAuthCmd(LoginCmd.cmd, LoginCmd.login);
        }
    }
}

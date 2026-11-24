package com.example.game.logic;

import com.example.game.action.LoginAction;
import com.example.game.exception.ErrorCode;
import com.iohao.game.action.skeleton.core.BarSkeleton;
import com.iohao.game.action.skeleton.core.BarSkeletonBuilder;
import com.iohao.game.action.skeleton.core.BarSkeletonBuilderParamConfig;
import com.iohao.game.action.skeleton.core.flow.internal.DebugInOut;
import com.iohao.game.bolt.broker.client.AbstractBrokerClientStartup;
import com.iohao.game.bolt.broker.core.client.BrokerAddress;
import com.iohao.game.bolt.broker.core.client.BrokerClient;
import com.iohao.game.bolt.broker.core.client.BrokerClientBuilder;
import com.iohao.game.bolt.broker.core.common.IoGameGlobalConfig;

/**
 * 游戏逻辑服。
 *
 * <p>ioGame 的逻辑服是「无状态业务节点」：它不直接监听客户端连接，
 * 而是注册到游戏网关，由对外服把请求转发进来。因此逻辑服可以水平扩容。
 */
public class GameLogicServer extends AbstractBrokerClientStartup {

    @Override
    public BarSkeleton createBarSkeleton() {
        BarSkeletonBuilderParamConfig config = new BarSkeletonBuilderParamConfig()
                // 扫描该包下所有 @ActionController，自动完成路由注册
                .scanActionPackage(LoginAction.class)
                // 注册业务错误码，框架据此把 code/msg 写回响应
                .addErrorCode(ErrorCode.values());

        BarSkeletonBuilder builder = config.createBuilder();

        // 控制台打印 路由/参数/返回值/耗时，开发期必备；上线建议关闭
        builder.addInOut(new DebugInOut());

        return builder.build();
    }

    @Override
    public BrokerClientBuilder createBrokerClientBuilder() {
        BrokerClientBuilder builder = BrokerClient.newBuilder();
        builder.appName("GameLogicServer");
        return builder;
    }

    @Override
    public BrokerAddress createBrokerAddress() {
        return new BrokerAddress("127.0.0.1", IoGameGlobalConfig.brokerPort);
    }
}

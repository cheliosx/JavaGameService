package com.example.game.logic;

import com.example.game.action.LoginAction;
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
                .scanActionPackage(LoginAction.class);

        BarSkeletonBuilder builder = config.createBuilder();

        // 业务错误码无需在此注册：ioGame 21.x 的 MsgException 自带 MsgExceptionInfo，
        // 抛出时框架直接读取 code 写回 responseStatus，而不是查注册表。
        // 旧写法 addErrorCode(MsgExceptionInfo[]) / addMsgExceptionInfo(...) 均已废弃，
        // 且其维护的 msgExceptionInfoList 在 build() 中从未被读取，保留只会带来 deprecated 告警。

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

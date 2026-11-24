package com.example.game.support;

import com.example.game.GameApplication;
import com.example.game.config.GameContext;
import com.example.game.logic.GameLogicServer;
import com.iohao.game.external.core.netty.simple.NettySimpleHelper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/**
 * 测试用服务器引导器：整个 JVM 只启动一次，所有用例共享。
 *
 * <p>端口用随机空闲端口，避免与开发者本机已在跑的 10100/10200 冲突。
 */
public final class TestServer {

    private static int externalPort = -1;

    private TestServer() {
    }

    public static synchronized int start() {
        if (externalPort > 0) {
            return externalPort;
        }

        GameContext.reset();

        int extPort = freePort();
        int brokerPort = freePort();

        GameApplication.configure(extPort, brokerPort);
        NettySimpleHelper.run(extPort, List.of(new GameLogicServer()));

        awaitPortOpen(extPort);

        externalPort = extPort;
        return externalPort;
    }

    public static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("无法分配空闲端口", e);
        }
    }

    private static void awaitPortOpen(int port) {
        long deadline = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < deadline) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("127.0.0.1", port), 200);
                return;
            } catch (IOException ignored) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }
        }
        throw new IllegalStateException("服务器启动超时，端口 " + port + " 未就绪");
    }
}

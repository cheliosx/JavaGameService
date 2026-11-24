package com.example.game.support;

import com.iohao.game.action.skeleton.core.CmdInfo;
import com.iohao.game.action.skeleton.core.DataCodecKit;
import com.iohao.game.action.skeleton.protocol.RequestMessage;
import com.iohao.game.external.core.message.ExternalCodecKit;
import com.iohao.game.external.core.message.ExternalMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试用极简客户端：直接用 JDK 自带 WebSocket 与 ioGame 的编解码器通信。
 *
 * <p>为什么不用官方 light-client：它是面向控制台交互的道具（继承
 * {@code AbstractInputCommandRegion}、靠 stdin 敲命令），不适合 JUnit 断言。
 * 这里只用框架公开的 {@code DataCodecKit} / {@code ExternalCodecKit}，
 * 与真实客户端走的字节完全一致，因此是真正的端到端测试。
 *
 * <p>帧格式（WebSocket）：整帧 = {@code DataCodecKit.encode(ExternalMessage)}，
 * <b>没有 4 字节长度前缀</b>（长度前缀是 TCP 才需要的，WS 自带消息边界）。
 */
public final class GameTestClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(GameTestClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private static final AtomicInteger SEQ = new AtomicInteger(1);

    private final WebSocket webSocket;
    private final BlockingQueue<byte[]> inbox;

    private GameTestClient(WebSocket webSocket, BlockingQueue<byte[]> inbox) {
        this.webSocket = webSocket;
        this.inbox = inbox;
    }

    /** ioGame 对外服 WebSocket 的握手路径 */
    public static final String DEFAULT_PATH = "/websocket";

    public static GameTestClient connect(int port) {
        return connect(port, DEFAULT_PATH);
    }

    public static GameTestClient connect(int port, String path) {
        BlockingQueue<byte[]> inbox = new LinkedBlockingQueue<>();

        WebSocket.Listener listener = new WebSocket.Listener() {
            private final ByteArrayOutputStream fragment = new ByteArrayOutputStream();

            @Override
            public CompletionStage<?> onBinary(WebSocket ws, ByteBuffer data, boolean last) {
                byte[] chunk = new byte[data.remaining()];
                data.get(chunk);
                fragment.writeBytes(chunk);
                if (last) {
                    inbox.add(fragment.toByteArray());
                    fragment.reset();
                }
                ws.request(1);
                return null;
            }

            @Override
            public void onError(WebSocket ws, Throwable error) {
                log.warn("测试客户端连接异常: {}", error.toString());
            }
        };

        WebSocket socket = HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .connectTimeout(TIMEOUT)
                .buildAsync(URI.create("ws://127.0.0.1:" + port + path), listener)
                .join();

        return new GameTestClient(socket, inbox);
    }

    /**
     * 发送请求并等待响应，返回原始 ExternalMessage。
     */
    public ExternalMessage request(int cmd, int subCmd, Object payload) {
        int msgId = SEQ.getAndIncrement();

        RequestMessage request = ExternalCodecKit.createRequest(CmdInfo.of(cmd, subCmd));
        if (payload != null) {
            request.setData(payload);
        }

        ExternalMessage external = ExternalCodecKit.convertExternalMessage(request);
        external.setMsgId(msgId);

        byte[] frame = DataCodecKit.encode(external);
        webSocket.sendBinary(ByteBuffer.wrap(frame), true).join();

        return await(msgId);
    }

    /**
     * 发送请求并把响应 data 反序列化为指定类型。
     */
    public <T> T request(int cmd, int subCmd, Object payload, Class<T> resultType) {
        ExternalMessage response = request(cmd, subCmd, payload);
        byte[] data = response.getData();
        if (data == null || data.length == 0) {
            return null;
        }
        return DataCodecKit.decode(data, resultType);
    }

    /**
     * 诊断用：发送请求后返回「下一条到达的消息」，不校验 msgId。
     */
    public ExternalMessage requestRaw(int cmd, int subCmd, Object payload, long timeoutMillis) {
        int msgId = SEQ.getAndIncrement();

        RequestMessage request = ExternalCodecKit.createRequest(CmdInfo.of(cmd, subCmd));
        if (payload != null) {
            request.setData(payload);
        }
        ExternalMessage external = ExternalCodecKit.convertExternalMessage(request);
        external.setMsgId(msgId);
        webSocket.sendBinary(ByteBuffer.wrap(DataCodecKit.encode(external)), true).join();

        try {
            byte[] bytes = inbox.poll(timeoutMillis, TimeUnit.MILLISECONDS);
            return bytes == null ? null : DataCodecKit.decode(bytes, ExternalMessage.class);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /**
     * 发送单向请求（对应服务端返回 {@code void} 的 action，不会有响应包）。
     */
    public void sendOneway(int cmd, int subCmd, Object payload) {
        int msgId = SEQ.getAndIncrement();

        RequestMessage request = ExternalCodecKit.createRequest(CmdInfo.of(cmd, subCmd));
        if (payload != null) {
            request.setData(payload);
        }
        ExternalMessage external = ExternalCodecKit.convertExternalMessage(request);
        external.setMsgId(msgId);

        webSocket.sendBinary(ByteBuffer.wrap(DataCodecKit.encode(external)), true).join();
    }

    /**
     * 在指定时间内是否收到了任何消息。
     *
     * <p>用于断言单向协议确实不回包。
     */
    public boolean receivedAny(Duration duration) {
        try {
            return inbox.poll(duration.toMillis(), TimeUnit.MILLISECONDS) != null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private ExternalMessage await(int msgId) {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            byte[] bytes;
            try {
                long remain = deadline - System.nanoTime();
                bytes = inbox.poll(Math.max(1, remain), TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("等待响应被中断", e);
            }

            if (bytes == null) {
                break;
            }

            ExternalMessage message = DataCodecKit.decode(bytes, ExternalMessage.class);
            // 严格按 msgId 匹配，避免把服务端主动推送的消息误当成本次响应
            if (message.getMsgId() == msgId) {
                return message;
            }
            log.debug("跳过非本次请求的消息 msgId={} cmdMerge={}",
                    message.getMsgId(), message.getCmdMerge());
        }
        throw new IllegalStateException("等待响应超时 msgId=" + msgId);
    }

    @Override
    public void close() {
        try {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "done").join();
        } catch (Exception e) {
            webSocket.abort();
        }
    }
}

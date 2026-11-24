package com.example.game.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 登录态（token）服务。
 *
 * <p>token 的用途：ioGame 的 userId 是绑定在「连接」上的，换一条连接（重连、切网络、换设备）
 * 就丢了。所以登录除了绑定连接，还要发一个可携带的凭证，供重连时恢复。
 *
 * <p>存储仍在内存，多实例部署时换成 Redis（key 加 TTL 即可，语义完全一致）。
 */
public class LoginSessionService {

    /** 默认有效期 24 小时 */
    public static final long DEFAULT_TTL_MILLIS = 24 * 60 * 60 * 1000L;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public LoginSessionService() {
        this(DEFAULT_TTL_MILLIS);
    }

    public LoginSessionService(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    /**
     * 为用户签发 token。同一用户重复登录会拿到新 token，旧 token 仍然有效（多端登录语义）。
     */
    public Session create(long userId) {
        cleanExpired();

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        long now = System.currentTimeMillis();
        Session session = new Session(token, userId, now, now + ttlMillis);
        sessions.put(token, session);
        return session;
    }

    /**
     * 校验 token。
     *
     * @return 有效则返回会话，否则 Optional.empty()
     */
    public java.util.Optional<Session> verify(String token) {
        if (token == null || token.isBlank()) {
            return java.util.Optional.empty();
        }

        Session session = sessions.get(token);
        if (session == null) {
            return java.util.Optional.empty();
        }

        if (session.isExpired(System.currentTimeMillis())) {
            sessions.remove(token);
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(session);
    }

    public void revoke(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    public void revokeByUserId(long userId) {
        sessions.values().removeIf(session -> session.userId() == userId);
    }

    public int size() {
        return sessions.size();
    }

    /**
     * 惰性清理过期会话，避免无上限增长。量大时应换成定时任务 + Redis TTL。
     */
    private void cleanExpired() {
        long now = System.currentTimeMillis();
        sessions.values().removeIf(session -> session.isExpired(now));
    }

    /**
     * 会话快照。
     */
    public record Session(String token, long userId, long createAt, long expireAt) {
        public boolean isExpired(long now) {
            return now >= expireAt;
        }
    }
}

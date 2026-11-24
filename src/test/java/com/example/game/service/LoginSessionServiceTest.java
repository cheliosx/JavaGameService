package com.example.game.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("登录态 token")
class LoginSessionServiceTest {

    @Test
    @DisplayName("签发的 token 可校验，且能取回 userId")
    void createAndVerify() {
        LoginSessionService service = new LoginSessionService();
        LoginSessionService.Session session = service.create(42L);

        assertTrue(session.token() != null && !session.token().isBlank());
        assertEquals(42L, service.verify(session.token()).orElseThrow().userId());
        assertTrue(session.expireAt() > System.currentTimeMillis());
    }

    @Test
    @DisplayName("每次签发的 token 都不同")
    void tokenIsUnique() {
        LoginSessionService service = new LoginSessionService();
        assertNotEquals(service.create(1L).token(), service.create(1L).token());
    }

    @Test
    @DisplayName("无效 token 一律校验失败")
    void rejectInvalidToken() {
        LoginSessionService service = new LoginSessionService();
        assertFalse(service.verify(null).isPresent());
        assertFalse(service.verify("").isPresent());
        assertFalse(service.verify("not-a-real-token").isPresent());
    }

    @Test
    @DisplayName("吊销后立即失效")
    void revoke() {
        LoginSessionService service = new LoginSessionService();
        String token = service.create(7L).token();

        service.revoke(token);
        assertFalse(service.verify(token).isPresent());
        assertEquals(0, service.size());
    }

    @Test
    @DisplayName("按用户吊销会清掉该用户所有 token（顶号/退登）")
    void revokeByUserId() {
        LoginSessionService service = new LoginSessionService();
        String first = service.create(9L).token();
        String second = service.create(9L).token();
        String other = service.create(10L).token();

        service.revokeByUserId(9L);

        assertFalse(service.verify(first).isPresent());
        assertFalse(service.verify(second).isPresent());
        assertTrue(service.verify(other).isPresent(), "不应影响其他用户");
    }

    @Test
    @DisplayName("过期 token 校验失败")
    void expire() {
        LoginSessionService service = new LoginSessionService(-1L);
        String token = service.create(1L).token();

        assertFalse(service.verify(token).isPresent());
    }

    @Test
    @DisplayName("过期会话会在下次签发时被清理，不会无限增长")
    void cleanExpired() {
        LoginSessionService service = new LoginSessionService(-1L);
        service.create(1L);
        assertEquals(1, service.size());

        service.create(2L);
        assertEquals(1, service.size(), "创建前应清掉已过期会话");
    }
}

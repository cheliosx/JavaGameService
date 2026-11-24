package com.example.game.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("用户服务 / 账号校验")
class UserServiceTest {

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService();
    }

    @Test
    @DisplayName("内置账号可用正确密码登录")
    void verifySuccess() {
        Optional<UserService.User> admin = userService.verify("admin", "admin123");
        assertTrue(admin.isPresent());
        assertEquals(1L, admin.get().userId());
        assertEquals("管理员", admin.get().nickname());

        Optional<UserService.User> player = userService.verify("player", "123456");
        assertTrue(player.isPresent());
        assertEquals(2L, player.get().userId());
    }

    @Test
    @DisplayName("密码错误返回空，且不区分「用户不存在」")
    void verifyWrongPassword() {
        assertFalse(userService.verify("admin", "wrong").isPresent());
        assertFalse(userService.verify("admin", "").isPresent());
        assertFalse(userService.verify("admin", null).isPresent());
    }

    @Test
    @DisplayName("用户不存在返回空")
    void verifyUnknownUser() {
        assertFalse(userService.verify("nobody", "admin123").isPresent());
        assertFalse(userService.verify(null, "admin123").isPresent());
    }

    @Test
    @DisplayName("按 id / 用户名查询")
    void findByIdAndName() {
        assertEquals("admin", userService.findById(1L).orElseThrow().username());
        assertEquals(2L, userService.findByUsername("player").orElseThrow().userId());
        assertFalse(userService.findById(999L).isPresent());
        assertFalse(userService.findByUsername("ghost").isPresent());
    }

    @Test
    @DisplayName("不存储明文密码")
    void passwordIsHashed() {
        String stored = userService.findById(1L).orElseThrow().passwordHash();
        assertFalse(stored.contains("admin123"), "存储中不允许出现明文密码");
        assertTrue(stored.startsWith("120000:"), "应带有迭代次数前缀");
    }

    @Test
    @DisplayName("注册新用户后可立即登录")
    void registerThenLogin() {
        userService.register(new UserService.User(
                100L, "newbie", "新人", PasswordEncoder.encode("pass1234")));

        assertEquals(100L, userService.verify("newbie", "pass1234").orElseThrow().userId());
        assertEquals(3, userService.count());
    }
}

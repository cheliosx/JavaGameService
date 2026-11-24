package com.example.game.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("密码哈希")
class PasswordEncoderTest {

    @Test
    @DisplayName("正确密码能通过校验")
    void matchesCorrectPassword() {
        String encoded = PasswordEncoder.encode("admin123");
        assertTrue(PasswordEncoder.matches("admin123", encoded));
    }

    @Test
    @DisplayName("错误密码无法通过校验")
    void rejectsWrongPassword() {
        String encoded = PasswordEncoder.encode("admin123");
        assertFalse(PasswordEncoder.matches("admin1234", encoded));
        assertFalse(PasswordEncoder.matches("", encoded));
        assertFalse(PasswordEncoder.matches("ADMIN123", encoded));
    }

    @Test
    @DisplayName("相同密码每次哈希结果不同（加盐生效）")
    void saltMakesHashesUnique() {
        String first = PasswordEncoder.encode("same-password");
        String second = PasswordEncoder.encode("same-password");

        assertNotEquals(first, second);
        assertTrue(PasswordEncoder.matches("same-password", first));
        assertTrue(PasswordEncoder.matches("same-password", second));
    }

    @Test
    @DisplayName("哈希串格式可解析且包含迭代次数")
    void encodedFormatIsSelfDescribing() {
        String encoded = PasswordEncoder.encode("whatever");
        String[] parts = encoded.split(":");
        assertTrue(parts.length == 3, "格式应为 iterations:salt:hash");
        assertTrue(Integer.parseInt(parts[0]) >= 100_000, "迭代次数不应低于 10 万");
    }

    @Test
    @DisplayName("脏数据不会抛异常，只返回 false")
    void malformedHashFailsSafely() {
        assertFalse(PasswordEncoder.matches("x", "not-a-hash"));
        assertFalse(PasswordEncoder.matches("x", "1:2:3"));
        assertFalse(PasswordEncoder.matches("x", null));
        assertFalse(PasswordEncoder.matches(null, PasswordEncoder.encode("x")));
    }

    @Test
    @DisplayName("空密码不允许编码")
    void rejectEmptyPassword() {
        assertThrows(IllegalArgumentException.class, () -> PasswordEncoder.encode(""));
        assertThrows(IllegalArgumentException.class, () -> PasswordEncoder.encode(null));
    }
}

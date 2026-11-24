package com.example.game.service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 用户服务。
 *
 * <p>脚手架阶段用内存 Map 模拟存储，接入 MySQL 时只需把三个方法换成 DAO 查询，
 * 上层 {@code LoginAction} 无需改动。
 *
 * <p>内置两个测试账号：
 * <ul>
 *     <li>admin / admin123（userId=1）</li>
 *     <li>player / 123456（userId=2）</li>
 * </ul>
 */
public class UserService {

    private final ConcurrentMap<String, User> usersByName = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, User> usersById = new ConcurrentHashMap<>();

    public UserService() {
        register(new User(1L, "admin", "管理员", PasswordEncoder.encode("admin123")));
        register(new User(2L, "player", "玩家一号", PasswordEncoder.encode("123456")));
    }

    /**
     * 注册用户（脚手架演示用，真实项目应由注册接口调用）。
     */
    public void register(User user) {
        usersByName.put(user.username(), user);
        usersById.put(user.userId(), user);
    }

    /**
     * 校验用户名密码。
     *
     * <p>用户不存在与密码错误返回同一个空结果 —— 防止攻击者通过错误码差异枚举出有效用户名。
     *
     * @return 校验通过返回用户，否则 Optional.empty()
     */
    public Optional<User> verify(String username, String rawPassword) {
        if (username == null || rawPassword == null) {
            return Optional.empty();
        }

        User user = usersByName.get(username);
        if (user == null) {
            // 即使用户不存在也走一次哈希运算，避免通过响应耗时差判断用户是否存在
            PasswordEncoder.matches(rawPassword, DUMMY_HASH);
            return Optional.empty();
        }

        if (!PasswordEncoder.matches(rawPassword, user.passwordHash())) {
            return Optional.empty();
        }

        return Optional.of(user);
    }

    public Optional<User> findById(long userId) {
        return Optional.ofNullable(usersById.get(userId));
    }

    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(usersByName.get(username));
    }

    public int count() {
        return usersByName.size();
    }

    /**
     * 一个固定的假哈希，用于把「用户不存在」的耗时对齐到「密码错误」。
     */
    private static final String DUMMY_HASH = PasswordEncoder.encode("dummy-password-for-timing");

    /**
     * 用户实体。passwordHash 为 PBKDF2 哈希串，不是明文。
     */
    public record User(long userId, String username, String nickname, String passwordHash) {
    }
}

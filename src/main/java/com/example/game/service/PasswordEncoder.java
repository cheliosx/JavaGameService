package com.example.game.service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密码哈希工具（PBKDF2-HMAC-SHA256 + 随机盐）。
 *
 * <p>为什么不用 MD5/SHA256(明文)：
 * 那类算法可被彩虹表与 GPU 秒级暴力破解。PBKDF2 通过高迭代次数把单次校验成本抬高，
 * 拖慢离线爆破速度。迭代次数会写入哈希串本身，便于后续平滑升级。
 *
 * <p>存储格式：{@code iterations:base64(salt):base64(hash)}
 */
public final class PasswordEncoder {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final String SEPARATOR = ":";

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordEncoder() {
    }

    /**
     * 生成密码哈希串。
     */
    public static String encode(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("rawPassword must not be empty");
        }
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);

        byte[] hash = pbkdf2(rawPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);

        Base64.Encoder base64 = Base64.getEncoder();
        return ITERATIONS + SEPARATOR + base64.encodeToString(salt) + SEPARATOR + base64.encodeToString(hash);
    }

    /**
     * 校验明文密码是否与哈希串匹配。
     *
     * <p>使用 {@link MessageDigest#isEqual} 做定长比较，避免因比较提前返回而泄漏信息。
     *
     * @return 任何格式异常都返回 false，不抛异常，避免把内部结构暴露给调用方
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }

        String[] parts = encodedPassword.split(SEPARATOR);
        if (parts.length != 3) {
            return false;
        }

        try {
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);
            if (expected.length == 0) {
                return false;
            }

            byte[] actual = pbkdf2(rawPassword.toCharArray(), salt, iterations, expected.length * 8);
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("PBKDF2 not available", e);
        } finally {
            spec.clearPassword();
        }
    }
}

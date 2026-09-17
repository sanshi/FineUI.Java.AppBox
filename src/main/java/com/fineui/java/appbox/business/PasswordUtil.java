package com.fineui.java.appbox.business;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码哈希工具：BCrypt（自带随机盐，哈希串内含 cost 因子，校验时按串内 cost 自适应）。
 *
 * <p>正常业务用默认强度（cost 10）；种子数据里约 200 个演示账号若也用 cost 10，首次启动要卡二十多秒，
 * 故种子专用低强度（cost 4）——校验端不区分两者。
 */
public final class PasswordUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();
    private static final BCryptPasswordEncoder SEED_ENCODER = new BCryptPasswordEncoder(4);

    private PasswordUtil() {
    }

    /** 明文 → 可入库的哈希串（业务侧新增用户 / 修改密码用）。 */
    public static String createDbPassword(String userPassword) {
        return ENCODER.encode(userPassword);
    }

    /** 明文 → 低强度哈希串（仅种子数据用，避免首启耗时）。 */
    public static String createSeedPassword(String userPassword) {
        return SEED_ENCODER.encode(userPassword);
    }

    /** 比对库中哈希与用户输入的明文。 */
    public static boolean comparePasswords(String dbPassword, String userPassword) {
        if (dbPassword == null || dbPassword.isEmpty() || userPassword == null) {
            return false;
        }
        return ENCODER.matches(userPassword, dbPassword);
    }
}

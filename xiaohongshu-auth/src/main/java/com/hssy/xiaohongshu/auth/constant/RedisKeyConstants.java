package com.hssy.xiaohongshu.auth.constant;

/**
 * @author 13759
 */
public class RedisKeyConstants {

    /**
     * 验证码 KEY 前缀
     */
    private static final String VERIFICATION_CODE_KEY_PREFIX = "verification_code:";

    /**
     * 小红书全局 ID 生成器 KEY
     */
    public static final String XIAOHONGSHU_ID_GENERATOR_KEY = "xiaohongshu_id_generator";

    /**
     * 用户角色数据 KEY 前缀
     */
    private static final String USER_ROLES_KEY_PREFIX = "user:roles:";

    /**
     * 角色对应的权限集合 KEY 前缀
     */
    private static final String ROLE_PERMISSIONS_KEY_PREFIX = "role:permissions:";


    /**
     * 构建角色对应的权限集合 KEY
     * @param roleId
     * @return
     */
    public static String buildRolePermissionsKey(Long roleId) {
        return ROLE_PERMISSIONS_KEY_PREFIX + roleId;
    }

    /**
     * 构建验证码 KEY
     * @param phone
     * @return
     */
    public static String buildVerificationCodeKey(String phone) {
        return VERIFICATION_CODE_KEY_PREFIX + phone;
    }

    /**
     * 构建用户角色关系 KEY
     * @param phone
     * @return
     */
    public static String buildUserRoleKey(String phone) {
        return USER_ROLES_KEY_PREFIX + phone;
    }
}
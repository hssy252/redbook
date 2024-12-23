package com.hssy.xiaohongshu.user.relation.biz.constants;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/22 18:56
 */
public class RedisKeyConstants {

    public static final String FOLLOW_USER_PREFIX = "following:";

    public static final String FANS_USER_PREFIX = "fans:";

    /**
     * 构建Redis中Zset的key
     * @param userId 发起关注操作的用户的id
     * @return
     */
    public static String buildFollowingUserKey(Long userId){
        return FOLLOW_USER_PREFIX + userId;
    }

    /**
     * 用于构建用户粉丝key，该key代表该用户有多少个粉丝
     * @param userId 用户的id
     * @return
     */
    public static String buildFansUserKey(Long userId){
        return FANS_USER_PREFIX + userId;
    }

}

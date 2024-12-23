package com.hssy.xiaohongshu.user.relation.biz.constants;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/22 19:01
 */
public interface MQConstants {

    /**
     * Topic: 关注、取关共用一个
     */
    String TOPIC_FOLLOW_OR_UNFOLLOW = "FollowUnfollowTopic";

    /**
     * 关注标签
     */
    String TAG_FOLLOW = "Follow";

    /**
     * 取关标签
     */
    String TAG_UNFOLLOW = "Unfollow";

}

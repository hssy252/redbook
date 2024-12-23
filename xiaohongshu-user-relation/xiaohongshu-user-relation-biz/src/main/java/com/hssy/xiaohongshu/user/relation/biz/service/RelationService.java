package com.hssy.xiaohongshu.user.relation.biz.service;

import com.hssy.framework.commom.response.Response;
import com.hssy.xiaohongshu.user.relation.biz.model.vo.FollowUserReqVO;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/22 18:54
 */
public interface RelationService {

    /**
     * 关注用户接口
     * @return
     */
    Response<?> followUser(FollowUserReqVO followUserReqVO);

}

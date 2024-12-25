package com.hssy.xiaohongshu.user.relation.biz.service;

import com.hssy.framework.commom.response.PageResponse;
import com.hssy.framework.commom.response.Response;
import com.hssy.xiaohongshu.user.relation.biz.model.vo.FindFansUserReqVO;
import com.hssy.xiaohongshu.user.relation.biz.model.vo.FindFansUserRspVO;
import com.hssy.xiaohongshu.user.relation.biz.model.vo.FindFollowingListReqVO;
import com.hssy.xiaohongshu.user.relation.biz.model.vo.FindFollowingUserRspVO;
import com.hssy.xiaohongshu.user.relation.biz.model.vo.FollowUserReqVO;
import com.hssy.xiaohongshu.user.relation.biz.model.vo.UnfollowUserReqVO;

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

    /**
     * 取关用户
     * @param unfollowUserReqVO
     * @return
     */
    Response<?> unfollow(UnfollowUserReqVO unfollowUserReqVO);

    /**
     * 查询关注列表
     * @param findFollowingListReqVO
     * @return
     */
    PageResponse<FindFollowingUserRspVO> findFollowingList(FindFollowingListReqVO findFollowingListReqVO);

    /**
     * 查询用户的粉丝列表
     * @param findFansUserReqVO
     * @return
     */
    PageResponse<FindFansUserRspVO> findFansList(FindFansUserReqVO findFansUserReqVO);

}

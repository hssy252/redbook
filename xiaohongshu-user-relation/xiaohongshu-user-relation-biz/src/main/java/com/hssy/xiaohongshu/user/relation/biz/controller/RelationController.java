package com.hssy.xiaohongshu.user.relation.biz.controller;

import com.hssy.framework.biz.operationlog.aspect.ApiOperationLog;
import com.hssy.framework.commom.response.PageResponse;
import com.hssy.framework.commom.response.Response;
import com.hssy.xiaohongshu.user.relation.biz.model.vo.FindFansUserReqVO;
import com.hssy.xiaohongshu.user.relation.biz.model.vo.FindFansUserRspVO;
import com.hssy.xiaohongshu.user.relation.biz.model.vo.FindFollowingListReqVO;
import com.hssy.xiaohongshu.user.relation.biz.model.vo.FindFollowingUserRspVO;
import com.hssy.xiaohongshu.user.relation.biz.model.vo.FollowUserReqVO;
import com.hssy.xiaohongshu.user.relation.biz.model.vo.UnfollowUserReqVO;
import com.hssy.xiaohongshu.user.relation.biz.service.RelationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/relation")
@Slf4j
public class RelationController {

    @Resource
    private RelationService relationService;

    @PostMapping("/follow")
    @ApiOperationLog(description = "关注用户")
    public Response<?> follow(@Validated @RequestBody FollowUserReqVO followUserReqVO) {
        return relationService.followUser(followUserReqVO);
    }

    @PostMapping("/unfollow")
    @ApiOperationLog(description = "取关用户")
    public Response<?> unfollow(@Validated @RequestBody UnfollowUserReqVO unfollowUserReqVO){
        return relationService.unfollow(unfollowUserReqVO);
    }

    @PostMapping("/following/list")
    @ApiOperationLog(description = "查询用户关注列表")
    public PageResponse<FindFollowingUserRspVO> findFollowingList(@Validated @RequestBody FindFollowingListReqVO findFollowingListReqVO) {
        return relationService.findFollowingList(findFollowingListReqVO);
    }

    @PostMapping("/fans/list")
    @ApiOperationLog(description = "查询用户粉丝列表")
    public PageResponse<FindFansUserRspVO> findFansList(@Validated @RequestBody FindFansUserReqVO findFansListReqVO) {
        return relationService.findFansList(findFansListReqVO);
    }


}
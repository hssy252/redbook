package com.hssy.xiaohongshu.user.relation.biz.service.impl;

import com.hssy.framework.biz.context.holder.LoginUserContextHolder;
import com.hssy.framework.commom.exception.BizException;
import com.hssy.framework.commom.response.Response;
import com.hssy.xiaohongshu.user.api.api.UserFeignApi;
import com.hssy.xiaohongshu.user.relation.biz.constants.FollowConstants;
import com.hssy.xiaohongshu.user.relation.biz.constants.MQConstants;
import com.hssy.xiaohongshu.user.relation.biz.constants.RedisKeyConstants;
import com.hssy.xiaohongshu.user.relation.biz.enums.ResponseCodeEnum;
import com.hssy.xiaohongshu.user.relation.biz.model.vo.FollowUserReqVO;
import com.hssy.xiaohongshu.user.relation.biz.rpc.UserRpcService;
import com.hssy.xiaohongshu.user.relation.biz.service.RelationService;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Objects;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/22 18:55
 */
@Service
public class RelationServiceImpl implements RelationService {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    @Resource
    private UserRpcService userRpcService;

    @Override
    public Response<?> followUser(FollowUserReqVO followUserReqVO) {
        // 判断被关注用户的id是不是自己
        Long userId = LoginUserContextHolder.getUserId();
        if (Objects.isNull(userId)){
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR);
        }
        Long followUserId = followUserReqVO.getFollowUserId();
        if (Objects.equals(userId,followUserId)){
            throw new BizException(ResponseCodeEnum.CANT_FOLLOW_YOURSELF);
        }

        // 判断用户是否存在
        Boolean existOrNot = userRpcService.userExistOrNot(userId);
        if (!Boolean.TRUE.equals(existOrNot)){
            throw new BizException(ResponseCodeEnum.FOLLOW_USER_NOT_EXIST);
        }

        // 用户关注数量是否达到上限,查询redis里的zset结构
        String followKey = RedisKeyConstants.buildFollowingUserKey(userId);
        // 得到该用户关注了多少人
        Long followCount = redisTemplate.opsForZSet().zCard(followKey);
        if (Objects.isNull(followCount)){
            throw new BizException(ResponseCodeEnum.PARAM_NOT_VALID);
        }
        if (followCount > FollowConstants.MAX_FOLLOW_USER_COUNT){
            throw new  BizException(ResponseCodeEnum.FOLLOW_USER_EXCEED_MAX_COUNT);
        }
        // 向redis添加关注和粉丝的记录
        // 构建被关注用户的粉丝key
        String fansKey = RedisKeyConstants.buildFansUserKey(followUserId);
        // 添加用户的关注信息： following:userId time followedUserId
        redisTemplate.opsForZSet().add(followKey,followUserId,LocalDateTime.now().atZone(ZoneId.of("Asia/Shanghai")).toEpochSecond());
        // 添加被关注用户的粉丝信息
        redisTemplate.opsForZSet().add(fansKey,userId,LocalDateTime.now().atZone(ZoneId.of("Asia/Shanghai")).toEpochSecond());

        // 然后通过MQ发消息，实现数据库异步入库（减轻数据库压力，削峰填谷
        rocketMQTemplate.syncSend(MQConstants.FOLLOW_USER_TOPIC, Collections.emptyMap());

        return Response.success();
    }
}

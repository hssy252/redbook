package com.hssy.xiaohongshu.user.relation.biz.service.impl;

import com.hssy.framework.biz.context.holder.LoginUserContextHolder;
import com.hssy.framework.commom.exception.BizException;
import com.hssy.framework.commom.response.Response;
import com.hssy.framework.commom.util.DateUtils;
import com.hssy.xiaohongshu.user.api.api.UserFeignApi;
import com.hssy.xiaohongshu.user.relation.biz.constants.FollowConstants;
import com.hssy.xiaohongshu.user.relation.biz.constants.MQConstants;
import com.hssy.xiaohongshu.user.relation.biz.constants.RedisKeyConstants;
import com.hssy.xiaohongshu.user.relation.biz.enums.LuaResultEnum;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
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

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        // Lua 脚本路径
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_check_and_add.lua")));
        // 返回值类型
        script.setResultType(Long.class);

        // 获取当前时间戳
        long timestamp = DateUtils.localDateTime2Timestamp(LocalDateTime.now());

        Long result = redisTemplate.execute(script, Collections.singletonList(followKey), followUserId, timestamp);

        LuaResultEnum luaResultEnum = LuaResultEnum.valueOf(result);

        if (Objects.isNull(luaResultEnum)) {
            throw new RuntimeException("Lua 返回结果错误");
        }

        // 判断返回结果
        switch (luaResultEnum) {
            // 关注数已达到上限
            case FOLLOW_LIMIT -> throw new BizException(ResponseCodeEnum.FOLLOW_USER_EXCEED_MAX_COUNT);
            // 已经关注了该用户
            case ALREADY_FOLLOWED -> throw new BizException(ResponseCodeEnum.ALREADY_FOLLOWED);
            // ZSet 关注列表不存在
            case ZSET_NOT_EXIST -> {
                // TODO

            }
        }


        // 然后通过MQ发消息，实现数据库异步入库（减轻数据库压力，削峰填谷
        rocketMQTemplate.syncSend(MQConstants.FOLLOW_USER_TOPIC, Collections.emptyMap());

        return Response.success();
    }
}

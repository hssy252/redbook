package com.hssy.xiaohongshu.user.relation.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import com.hssy.framework.biz.context.holder.LoginUserContextHolder;
import com.hssy.framework.commom.exception.BizException;
import com.hssy.framework.commom.response.Response;
import com.hssy.framework.commom.util.DateUtils;
import com.hssy.framework.commom.util.JsonUtils;
import com.hssy.xiaohongshu.user.api.api.UserFeignApi;
import com.hssy.xiaohongshu.user.relation.biz.constants.FollowConstants;
import com.hssy.xiaohongshu.user.relation.biz.constants.MQConstants;
import com.hssy.xiaohongshu.user.relation.biz.constants.RedisKeyConstants;
import com.hssy.xiaohongshu.user.relation.biz.domain.dataobject.FollowingDO;
import com.hssy.xiaohongshu.user.relation.biz.domain.mapper.FollowingDOMapper;
import com.hssy.xiaohongshu.user.relation.biz.enums.LuaResultEnum;
import com.hssy.xiaohongshu.user.relation.biz.enums.ResponseCodeEnum;
import com.hssy.xiaohongshu.user.relation.biz.model.dto.FollowUserMqDTO;
import com.hssy.xiaohongshu.user.relation.biz.model.vo.FollowUserReqVO;
import com.hssy.xiaohongshu.user.relation.biz.rpc.UserRpcService;
import com.hssy.xiaohongshu.user.relation.biz.service.RelationService;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
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
@Slf4j
public class RelationServiceImpl implements RelationService {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    @Resource
    private UserRpcService userRpcService;

    @Resource
    private FollowingDOMapper followingDOMapper;

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
        LocalDateTime now = LocalDateTime.now();
        long timestamp = DateUtils.localDateTime2Timestamp(now);

        Long result = redisTemplate.execute(script, Collections.singletonList(followKey), followUserId, timestamp);

        // 校验 Lua 脚本执行结果
        checkLuaScriptResult(result);

        // ZSET 不存在
        if (Objects.equals(result, LuaResultEnum.ZSET_NOT_EXIST.getCode())) {
            // 从数据库查询当前用户的关注关系记录
            List<FollowingDO> followingDOS = followingDOMapper.selectByUserId(userId);

            // 随机过期时间
            // 保底1天+随机秒数
            long expireSeconds = 60*60*24 + RandomUtil.randomInt(60*60*24);

            // 若记录为空，直接 ZADD 对象, 并设置过期时间
            if (CollUtil.isEmpty(followingDOS)) {
                DefaultRedisScript<Long> script2 = new DefaultRedisScript<>();
                script2.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_add_and_expire.lua")));
                script2.setResultType(Long.class);

                // TODO: 可以根据用户类型，设置不同的过期时间，若当前用户为大V, 则可以过期时间设置的长些或者不设置过期时间；如不是，则设置的短些
                // 如何判断呢？可以从计数服务获取用户的粉丝数，目前计数服务还没创建，则暂时采用统一的过期策略
                redisTemplate.execute(script2, Collections.singletonList(followKey), followUserId, timestamp, expireSeconds);
            } else { // 若记录不为空，则将关注关系数据全量同步到 Redis 中，并设置过期时间；
                // 构建 Lua 参数
                Object[] luaArgs = buildLuaArgs(followingDOS, expireSeconds);

                // 执行 Lua 脚本，批量同步关注关系数据到 Redis 中
                DefaultRedisScript<Long> script3 = new DefaultRedisScript<>();
                script3.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_batch_add_and_expire.lua")));
                script3.setResultType(Long.class);
                redisTemplate.execute(script3, Collections.singletonList(followKey), luaArgs);

                // 再次调用上面的 Lua 脚本：follow_check_and_add.lua , 将最新的关注关系添加进去
                result = redisTemplate.execute(script, Collections.singletonList(followKey), followUserId, timestamp);
                checkLuaScriptResult(result);
            }
        }

        // 然后通过MQ发消息，实现数据库异步入库（减轻数据库压力，削峰填谷
        FollowUserMqDTO mqDTO = FollowUserMqDTO.builder()
            .userId(userId)
            .followUserId(followUserId)
            .createTime(now)
            .build();

        // 构建消息对象
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(mqDTO)).build();

        // 构建Topic和Tag
        String destination = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW + ":" + MQConstants.TAG_FOLLOW;

        log.info("开始发送消息实体: {}",mqDTO);

        // 异步发送MQ消息
        rocketMQTemplate.asyncSend(destination, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("MQ消息发送成功，发送结果：{}",sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("MQ消息发送失败，发送结果：",throwable);
            }
        });

        return Response.success();
    }


    /**
     * 校验 Lua 脚本结果，根据状态码抛出对应的业务异常
     * @param result
     */
    private static void checkLuaScriptResult(Long result) {
        LuaResultEnum luaResultEnum = LuaResultEnum.valueOf(result);

        if (Objects.isNull(luaResultEnum)) throw new RuntimeException("Lua 返回结果错误");
        // 校验 Lua 脚本执行结果
        switch (luaResultEnum) {
            // 关注数已达到上限
            case FOLLOW_LIMIT -> throw new BizException(ResponseCodeEnum.FOLLOW_USER_EXCEED_MAX_COUNT);
            // 已经关注了该用户
            case ALREADY_FOLLOWED -> throw new BizException(ResponseCodeEnum.ALREADY_FOLLOWED);
        }
    }

    /**
     * 构建 Lua 脚本参数
     *
     * @param followingDOS
     * @param expireSeconds
     * @return
     */
    private static Object[] buildLuaArgs(List<FollowingDO> followingDOS, long expireSeconds) {
        int argsLength = followingDOS.size() * 2 + 1; // 每个关注关系有 2 个参数（score 和 value），再加一个过期时间
        Object[] luaArgs = new Object[argsLength];

        int i = 0;
        for (FollowingDO following : followingDOS) {
            luaArgs[i] = DateUtils.localDateTime2Timestamp(following.getCreateTime()); // 关注时间作为 score
            luaArgs[i + 1] = following.getFollowingUserId();          // 关注的用户 ID 作为 ZSet value
            i += 2;
        }

        luaArgs[argsLength - 1] = expireSeconds; // 最后一个参数是 ZSet 的过期时间
        return luaArgs;
    }
}

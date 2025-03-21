package com.hssy.xiaohongshu.user.relation.biz.consumer;

import com.google.common.util.concurrent.RateLimiter;
import com.hssy.framework.commom.util.JsonUtils;
import com.hssy.xiaohongshu.user.relation.biz.constants.MQConstants;
import com.hssy.xiaohongshu.user.relation.biz.constants.RedisKeyConstants;
import com.hssy.xiaohongshu.user.relation.biz.domain.dataobject.FansDO;
import com.hssy.xiaohongshu.user.relation.biz.domain.dataobject.FollowingDO;
import com.hssy.xiaohongshu.user.relation.biz.domain.mapper.FansDOMapper;
import com.hssy.xiaohongshu.user.relation.biz.domain.mapper.FollowingDOMapper;
import com.hssy.xiaohongshu.user.relation.biz.enums.FollowUnfollowTypeEnum;
import com.hssy.xiaohongshu.user.relation.biz.model.dto.CountFollowUnfollowMqDTO;
import com.hssy.xiaohongshu.user.relation.biz.model.dto.FollowUserMqDTO;
import com.hssy.xiaohongshu.user.relation.biz.model.dto.UnfollowUserMqDTO;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/23 16:43
 */

@Component
@RocketMQMessageListener(consumerGroup = "xiaohongshu_group_" + MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW,
    topic = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW,
    consumeMode = ConsumeMode.ORDERLY
)
@Slf4j
public class FollowUnFollowConsumer implements RocketMQListener<Message> {

    @Resource
    private FollowingDOMapper followingDOMapper;
    @Resource
    private FansDOMapper fansDOMapper;
    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    // 每秒创建5000个令牌
    @Resource
    private RateLimiter rateLimiter;

    @Resource
    private RedisTemplate<String,Objects> redisTemplate;


    @Override
    public void onMessage(Message message) {
        // 流量削峰：通过获取令牌，如果没有令牌可用，将阻塞，直到获得
        rateLimiter.acquire();

        // 获得消息体
        String bodyJsonStr = new String(message.getBody());

        // 获得标签
        String tags = message.getTags();

        log.info("==> FollowUnfollowConsumer 消费了消息 {}, tags: {}", bodyJsonStr, tags);

        // 根据 MQ 标签，判断操作类型
        if (Objects.equals(tags, MQConstants.TAG_FOLLOW)) { // 关注
            handleFollowTagMessage(bodyJsonStr);
        } else if (Objects.equals(tags, MQConstants.TAG_UNFOLLOW)) { // 取关
            handleUnfollowTagMessage(bodyJsonStr);
        }

    }

    /**
     * 关注用户
     * @param bodyJsonStr
     */
    private void handleFollowTagMessage(String bodyJsonStr){
        // 将消息体转换为对象
        FollowUserMqDTO followUserMqDTO = JsonUtils.parseObject(bodyJsonStr, FollowUserMqDTO.class);

        // 判空
        if (Objects.isNull(followUserMqDTO)) return;

        // 幂等性判断

        Long followUserId = followUserMqDTO.getFollowUserId();
        Long userId = followUserMqDTO.getUserId();
        LocalDateTime createTime = followUserMqDTO.getCreateTime();

        // 编程式提交事务
        boolean isSuccess = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            try {
                // 关注成功需往数据库添加两条记录
                // 关注表：一条记录
                int count = followingDOMapper.insert(FollowingDO.builder()
                    .userId(userId)
                    .followingUserId(followUserId)
                    .createTime(createTime)
                    .build());

                // 粉丝表：一条记录
                if (count > 0) {
                    fansDOMapper.insert(FansDO.builder()
                        .userId(followUserId)
                        .fansUserId(userId)
                        .createTime(createTime)
                        .build());
                }
                return true;
            } catch (Exception ex) {
                status.setRollbackOnly(); // 标记事务为回滚
                log.error("", ex);
            }
            return false;
        }));

        log.info("## 数据库添加记录结果：{}", isSuccess);
        // 更新 Redis 中被关注用户的 ZSet 粉丝列表
        if (isSuccess){
            String fansKey = RedisKeyConstants.buildFansUserKey(followUserId);

            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/follow_check_and_update_fans_zset.lua")));
            script.setResultType(Long.class);
            redisTemplate.execute(script, Collections.singletonList(fansKey), userId, createTime);
            
            // 发送计数消息
            CountFollowUnfollowMqDTO mqDTO = CountFollowUnfollowMqDTO.builder()
                .userId(userId)
                .targetUserId(followUserId)
                .type(FollowUnfollowTypeEnum.FOLLOW.getCode())
                .build();
            
            sendMQ(mqDTO);

        }
    }

    /**
     * 取关
     * @param bodyJsonStr
     */
    private void handleUnfollowTagMessage(String bodyJsonStr) {
        // 将消息体 Json 字符串转为 DTO 对象
        UnfollowUserMqDTO unfollowUserMqDTO = JsonUtils.parseObject(bodyJsonStr, UnfollowUserMqDTO.class);

        // 判空
        if (Objects.isNull(unfollowUserMqDTO)) return;

        Long userId = unfollowUserMqDTO.getUserId();
        Long unfollowUserId = unfollowUserMqDTO.getUnfollowUserId();
        LocalDateTime createTime = unfollowUserMqDTO.getCreateTime();

        // 编程式提交事务
        boolean isSuccess = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            try {
                // 取关成功需要删除数据库两条记录
                // 关注表：一条记录
                int count = followingDOMapper.deleteByUserIdAndFollowingUserId(userId, unfollowUserId);

                // 粉丝表：一条记录
                if (count > 0) {
                    fansDOMapper.deleteByUserIdAndFansUserId(unfollowUserId, userId);
                }
                return true;
            } catch (Exception ex) {
                status.setRollbackOnly(); // 标记事务为回滚
                log.error("", ex);
            }
            return false;
        }));

        // 若数据库删除成功，更新 Redis，将自己从被取注用户的 ZSet 粉丝列表删除
        if (isSuccess) {
            // 被取关用户的粉丝列表 Redis Key
            String fansRedisKey = RedisKeyConstants.buildFansUserKey(unfollowUserId);
            // 删除指定粉丝
            redisTemplate.opsForZSet().remove(fansRedisKey, userId);

            // 发送MQ
            CountFollowUnfollowMqDTO mqDTO = CountFollowUnfollowMqDTO.builder()
                .userId(userId)
                .targetUserId(unfollowUserId)
                .type(FollowUnfollowTypeEnum.UNFOLLOW.getCode())
                .build();

            sendMQ(mqDTO);
        }
    }

    private void sendMQ(CountFollowUnfollowMqDTO mqDTO) {
        org.springframework.messaging.Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(mqDTO)).build();

        // 异步发送 MQ 消息
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_FOLLOWING, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务：关注数】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数服务：关注数】MQ 发送异常: ", throwable);
            }
        });

        // 发送 MQ 通知计数服务：统计粉丝数
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_FANS, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务：粉丝数】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数服务：粉丝数】MQ 发送异常: ", throwable);
            }
        });

    }


}

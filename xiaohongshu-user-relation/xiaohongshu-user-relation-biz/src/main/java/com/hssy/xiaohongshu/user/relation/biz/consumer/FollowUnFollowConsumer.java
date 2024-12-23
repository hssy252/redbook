package com.hssy.xiaohongshu.user.relation.biz.consumer;

import com.google.common.util.concurrent.RateLimiter;
import com.hssy.framework.commom.util.JsonUtils;
import com.hssy.xiaohongshu.user.relation.biz.constants.MQConstants;
import com.hssy.xiaohongshu.user.relation.biz.domain.dataobject.FansDO;
import com.hssy.xiaohongshu.user.relation.biz.domain.dataobject.FollowingDO;
import com.hssy.xiaohongshu.user.relation.biz.domain.mapper.FansDOMapper;
import com.hssy.xiaohongshu.user.relation.biz.domain.mapper.FollowingDOMapper;
import com.hssy.xiaohongshu.user.relation.biz.model.dto.FollowUserMqDTO;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
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
@RocketMQMessageListener(consumerGroup = "xiaohongshu_group",
    topic = MQConstants.TOPIC_FOLLOW_OR_UNFOLLOW
)
@Slf4j
public class FollowUnFollowConsumer implements RocketMQListener<Message> {

    @Resource
    private FollowingDOMapper followingDOMapper;
    @Resource
    private FansDOMapper fansDOMapper;
    @Resource
    private TransactionTemplate transactionTemplate;

    // 每秒创建5000个令牌
    @Resource
    private RateLimiter rateLimiter;

    //  省略...


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
            // TODO
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
        // TODO: 更新 Redis 中被关注用户的 ZSet 粉丝列表
    }

}

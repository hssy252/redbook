package com.hssy.xiaohongshu.count.biz.consumer;

import com.github.phantomthief.collection.BufferTrigger;
import com.google.common.collect.Maps;
import com.hssy.framework.commom.util.JsonUtils;
import com.hssy.xiaohongshu.count.biz.constants.MQConstants;
import com.hssy.xiaohongshu.count.biz.constants.RedisKeyConstants;
import com.hssy.xiaohongshu.count.biz.enums.FollowUnfollowTypeEnum;
import com.hssy.xiaohongshu.count.biz.model.dto.CountFollowUnfollowMqDTO;
import jakarta.annotation.Resource;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/28 19:32
 */

@Component
@RocketMQMessageListener(consumerGroup = "xiaohongshu_group_" + MQConstants.TOPIC_COUNT_FANS,
    topic = MQConstants.TOPIC_COUNT_FANS
)
@Slf4j
public class CountFansConsumer implements RocketMQListener<String> {

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private final BufferTrigger<String> bufferTrigger = BufferTrigger.<String>batchBlocking()
        .bufferSize(50000) // 缓存队列的最大容量
        .batchSize(1000)   // 一批次最多聚合 1000 条
        .linger(Duration.ofSeconds(1)) // 多久聚合一次
        .setConsumerEx(this::consumeMessage)
        .build();

    @Override
    public void onMessage(String body) {
        // 往 bufferTrigger 中添加元素
        bufferTrigger.enqueue(body);
    }

    private void consumeMessage(List<String> bodys) {
        log.info("==> 聚合消息, size: {}", bodys.size());
        log.info("==> 聚合消息, {}", JsonUtils.toJsonString(bodys));

        // 将json转化成dto对象
        List<CountFollowUnfollowMqDTO> list = bodys.stream()
            .map(json -> JsonUtils.parseObject(json, CountFollowUnfollowMqDTO.class))
            .toList();

        // 将消息按目标用户分组
        Map<Long, List<CountFollowUnfollowMqDTO>> groupMap = list.stream().collect(Collectors.groupingBy(CountFollowUnfollowMqDTO::getTargetUserId));

        // 按组汇总数据，统计出最终的计数
        // key 为目标用户ID, value 为最终操作的计数
        Map<Long, Integer> countMap = Maps.newHashMap();

        for (Entry<Long, List<CountFollowUnfollowMqDTO>> entry : groupMap.entrySet()) {
            // 用户的id,
            Long key = entry.getKey();
            Integer count = 0;
            for (CountFollowUnfollowMqDTO mqDTO : entry.getValue()) {
                // 对每一个用户进行聚合整理
                FollowUnfollowTypeEnum typeEnum = FollowUnfollowTypeEnum.valueOf(mqDTO.getType());
                if (Objects.isNull(typeEnum)){
                    continue;
                }

                switch (typeEnum){
                    // 若是关注操作，则目标用户的粉丝数加1，反之则减1
                    case FOLLOW -> count++;
                    case UNFOLLOW -> count--;
                }
            }

            // 将聚合后的结果放入map中
            countMap.put(key,count);
        }

        log.info("## 聚合后的计数数据: {}", JsonUtils.toJsonString(countMap));

        // 将map中的结果进行redis的缓存操作

        // 更新 Redis
        countMap.forEach((k, v) -> {
            // Redis Key
            String redisKey = RedisKeyConstants.buildCountUserKey(k);
            // 判断 Redis 中 Hash 是否存在
            boolean isExisted = redisTemplate.hasKey(redisKey);

            // 若存在才会更新
            // (因为缓存设有过期时间，考虑到过期后，缓存会被删除，这里需要判断一下，存在才会去更新，而初始化工作放在查询计数来做)
            if (isExisted) {
                // 对目标用户 Hash 中的粉丝数字段进行计数操作
                redisTemplate.opsForHash().increment(redisKey, RedisKeyConstants.FIELD_FANS_TOTAL, v);
            }
        });

        // 发送 MQ, 计数数据落库
        // 构建消息体 DTO
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(countMap))
            .build();

        // 异步发送 MQ 消息，提升接口响应速度
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_FANS_2_DB, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务：粉丝数入库】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数服务：粉丝数入库】MQ 发送异常: ", throwable);
            }
        });


    }
}

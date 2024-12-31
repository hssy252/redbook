package com.hssy.xiaohongshu.count.biz.consumer;

import com.github.phantomthief.collection.BufferTrigger;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hssy.framework.commom.util.JsonUtils;
import com.hssy.xiaohongshu.count.biz.constants.MQConstants;
import com.hssy.xiaohongshu.count.biz.constants.RedisKeyConstants;
import com.hssy.xiaohongshu.count.biz.enums.LikeUnlikeNoteTypeEnum;
import com.hssy.xiaohongshu.count.biz.model.dto.AggregationCountLikeUnlikeNoteMqDTO;
import com.hssy.xiaohongshu.count.biz.model.dto.CountLikeUnlikeNoteMqDTO;
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
 * @author 13759
 */
@Component
@RocketMQMessageListener(consumerGroup = "xiaohongshu_group_" + MQConstants.TOPIC_COUNT_NOTE_LIKE, // Group 组
        topic = MQConstants.TOPIC_COUNT_NOTE_LIKE // 主题 Topic
        )
@Slf4j
public class CountNoteLikeConsumer implements RocketMQListener<String> {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private BufferTrigger<String> bufferTrigger = BufferTrigger.<String>batchBlocking()
            .bufferSize(50000) // 缓存队列的最大容量
            .batchSize(1000)   // 一批次最多聚合 1000 条
            .linger(Duration.ofSeconds(1)) // 多久聚合一次
            .setConsumerEx(this::consumeMessage) // 设置消费者方法
            .build();

    @Override
    public void onMessage(String body) {
        // 往 bufferTrigger 中添加元素
        bufferTrigger.enqueue(body);
    }

    private void consumeMessage(List<String> bodys) {
        log.info("==> 【笔记点赞数】聚合消息, size: {}", bodys.size());
        log.info("==> 【笔记点赞数】聚合消息, {}", JsonUtils.toJsonString(bodys));

        // 将json字符串转化为dto
        List<CountLikeUnlikeNoteMqDTO> list = bodys.stream()
            .map(str -> JsonUtils.parseObject(str, CountLikeUnlikeNoteMqDTO.class))
            .toList();

        // 根据笔记id分组，方便聚合操作,key 为笔记id
        Map<Long, List<CountLikeUnlikeNoteMqDTO>> groupMap = list.stream()
            .collect(Collectors.groupingBy(CountLikeUnlikeNoteMqDTO::getNoteId));

        // 按组汇总数据，统计出最终的计数
        // 最终操作的计数对象
        List<AggregationCountLikeUnlikeNoteMqDTO> countList = Lists.newArrayList();


        for (Entry<Long, List<CountLikeUnlikeNoteMqDTO>> entry : groupMap.entrySet()) {
            Long key = entry.getKey();
            List<CountLikeUnlikeNoteMqDTO> value = entry.getValue();
            // 笔记发布者 ID
            Long creatorId = value.get(0).getNoteCreatorId();
            int count = 0;
            for (CountLikeUnlikeNoteMqDTO mqDTO : value) {
                Integer type = mqDTO.getType();
                LikeUnlikeNoteTypeEnum likeUnlikeNoteTypeEnum = LikeUnlikeNoteTypeEnum.valueOf(type);
                if (Objects.isNull(likeUnlikeNoteTypeEnum)) continue;
                switch (likeUnlikeNoteTypeEnum){
                    case LIKE -> count++;
                    case UNLIKE -> count--;
                }
            }
            // 将分组后统计出的最终计数，存入 countList 中
            countList.add(AggregationCountLikeUnlikeNoteMqDTO.builder()
                .noteId(key)
                .creatorId(creatorId)
                .count(count)
                .build());
        }

        log.info("## 【笔记点赞数】聚合后的计数数据: {}", JsonUtils.toJsonString(countList));
        // 更新 Redis
        countList.forEach(item -> {
            // 笔记发布者 ID
            Long creatorId = item.getCreatorId();
            // 笔记 ID
            Long noteId = item.getNoteId();
            // 聚合后的计数
            Integer count = item.getCount();

            // 笔记维度计数 Redis Key
            String countNoteRedisKey = RedisKeyConstants.buildCountNoteKey(noteId);
            // 判断 Redis 中 Hash 是否存在
            boolean isCountNoteExisted = redisTemplate.hasKey(countNoteRedisKey);

            // 若存在才会更新
            // (因为缓存设有过期时间，考虑到过期后，缓存会被删除，这里需要判断一下，存在才会去更新，而初始化工作放在查询计数来做)
            if (isCountNoteExisted) {
                // 对目标用户 Hash 中的点赞数字段进行计数操作
                redisTemplate.opsForHash().increment(countNoteRedisKey, RedisKeyConstants.FIELD_LIKE_TOTAL, count);
            }

            // 更新 Redis 用户维度点赞数
            String countUserRedisKey = RedisKeyConstants.buildCountUserKey(creatorId);
            boolean isCountUserExisted = redisTemplate.hasKey(countUserRedisKey);
            if (isCountUserExisted) {
                redisTemplate.opsForHash().increment(countUserRedisKey, RedisKeyConstants.FIELD_LIKE_TOTAL, count);
            }
        });

        // 发送 MQ, 笔记点赞数据落库
        Message<String> message = MessageBuilder.withPayload(JsonUtils.toJsonString(countList))
            .build();

        // 异步发送 MQ 消息
        rocketMQTemplate.asyncSend(MQConstants.TOPIC_COUNT_NOTE_LIKE_2_DB, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("==> 【计数服务：笔记点赞数入库】MQ 发送成功，SendResult: {}", sendResult);
            }

            @Override
            public void onException(Throwable throwable) {
                log.error("==> 【计数服务：笔记点赞数入库】MQ 发送异常: ", throwable);
            }
        });

    }
}
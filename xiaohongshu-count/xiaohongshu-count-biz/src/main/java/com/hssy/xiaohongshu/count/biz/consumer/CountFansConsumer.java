package com.hssy.xiaohongshu.count.biz.consumer;

import com.github.phantomthief.collection.BufferTrigger;
import com.hssy.framework.commom.util.JsonUtils;
import com.hssy.xiaohongshu.count.biz.constants.MQConstants;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
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
    }
}

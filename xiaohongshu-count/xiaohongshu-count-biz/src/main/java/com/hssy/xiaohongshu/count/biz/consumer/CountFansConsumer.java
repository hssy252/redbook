package com.hssy.xiaohongshu.count.biz.consumer;

import com.hssy.xiaohongshu.count.biz.constants.MQConstants;
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

    @Override
    public void onMessage(String body) {
        log.info("## 消费到了 MQ 【计数: 粉丝数】, {}...", body);
    }
}

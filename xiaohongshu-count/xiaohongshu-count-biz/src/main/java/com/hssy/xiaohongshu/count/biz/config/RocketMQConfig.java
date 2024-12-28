package com.hssy.xiaohongshu.count.biz.config;

import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/28 17:14
 */

@Configuration
@Import(RocketMQAutoConfiguration.class)
public class RocketMQConfig {

}

package com.hssy.xiaohongshu.user.relation.biz.config;

import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/23 16:32
 */

@Configuration
@Import(RocketMQAutoConfiguration.class)
public class RocketMQConfig {

}

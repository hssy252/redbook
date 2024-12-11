package com.hssy.xiaohongshu.auth.sms;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.teaopenapi.models.Config;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/11 17:51
 */

@Configuration
@Slf4j
public class AliyunSmsClientConfig {

    @Resource
    private AliyunAccessKeyProperties aliyunAccessKeyProperties;

    @Bean
    public Client smsClient() {
        try {
            Config config = new Config()
                // 必填
                .setAccessKeyId(aliyunAccessKeyProperties.getAccessKeyId())
                // 必填
                .setAccessKeySecret(aliyunAccessKeyProperties.getAccessKeySecret());

            // Endpoint 请参考 https://api.aliyun.com/product/Dysmsapi
            config.endpoint = "dysmsapi.aliyuncs.com";

            return new Client(config);
        } catch (Exception e) {
            log.error("初始化阿里云短信发送客户端错误: ", e);
            return null;
        }
    }

}

package com.hssy.xiaohongshu.oss.biz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/16 20:24
 */
@ConfigurationProperties(prefix = "aliyun-oss")
@Component
@Data
public class AliyunOSSProperties {

    private String accessKey;

    private String secretKey;

    private String endpoint;

    private String bucketName;

}

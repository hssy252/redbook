package com.hssy.xiaohongshu.oss.biz.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/16 19:39
 */
@ConfigurationProperties(prefix = "minio")
@Data
@Component
public class MinioProperties {

    private String accessKey;

    private String secretKey;

    private String endpoint;

    private String bucketName;

}

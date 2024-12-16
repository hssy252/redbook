package com.hssy.xiaohongshu.oss.biz.config;

import io.minio.MinioClient;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/16 19:42
 */

@Configuration
public class MinioConfig {

    @Resource
    private MinioProperties minioProperties;

    @Bean
    public MinioClient minioClient(){
        return MinioClient.builder()
            .endpoint(minioProperties.getEndpoint())
            .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
            .build();
    }

}

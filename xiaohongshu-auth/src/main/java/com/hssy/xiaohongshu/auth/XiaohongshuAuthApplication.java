package com.hssy.xiaohongshu.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("com.hssy.xiaohongshu.auth.domain.mapper")
@EnableFeignClients(basePackages = "com.hssy.xiaohongshu")
public class XiaohongshuAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaohongshuAuthApplication.class, args);
    }

}

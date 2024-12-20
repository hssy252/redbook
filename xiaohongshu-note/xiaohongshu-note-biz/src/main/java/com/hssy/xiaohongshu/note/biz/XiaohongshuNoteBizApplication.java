package com.hssy.xiaohongshu.note.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @author 13759
 */
@SpringBootApplication
@MapperScan("com.hssy.xiaohongshu.note.biz.domain.mapper")
@EnableFeignClients(basePackages = "com.hssy.xiaohongshu")
public class XiaohongshuNoteBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaohongshuNoteBizApplication.class, args);
    }

}
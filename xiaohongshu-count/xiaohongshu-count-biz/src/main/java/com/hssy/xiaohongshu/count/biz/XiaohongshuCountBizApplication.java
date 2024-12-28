package com.hssy.xiaohongshu.count.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/28 17:05
 */

@SpringBootApplication
@MapperScan("com.hssy.xiaohongshu.count.biz.domain.mapper")
public class XiaohongshuCountBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaohongshuCountBizApplication.class,args);
    }

}

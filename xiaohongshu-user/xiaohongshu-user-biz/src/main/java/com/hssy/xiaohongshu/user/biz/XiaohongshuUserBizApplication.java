package com.hssy.xiaohongshu.user.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/16 20:51
 */
@SpringBootApplication
@MapperScan("com.hssy.xiaohongshu.user.biz.domain.mapper")
public class XiaohongshuUserBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaohongshuUserBizApplication.class,args);
    }

}

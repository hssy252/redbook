package com.hssy.xiaohongshu.user.relation.biz;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/22 13:39
 */

@SpringBootApplication
@MapperScan("com.hssy.xiaohongshu.user.relation.biz.domain.mapper")
public class XiaohongshuUserRelationBizApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiaohongshuUserRelationBizApplication.class,args);
    }
}

package com.hssy.xiaohongshu.oss.config;

import feign.codec.Encoder;
import feign.form.spring.SpringFormEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 功能简述
 * 解决Feign接口传输文件参数的问题
 * @author hssy
 * @version 1.0
 * @since 2024/12/17 18:21
 */
@Configuration
public class FeignFormConfig {

    @Bean
    public Encoder feignFormEncoder(){
        return new SpringFormEncoder();
    }

}

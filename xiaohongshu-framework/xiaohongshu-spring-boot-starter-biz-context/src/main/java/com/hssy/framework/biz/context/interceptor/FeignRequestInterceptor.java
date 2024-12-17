package com.hssy.framework.biz.context.interceptor;

import com.hssy.framework.biz.context.holder.LoginUserContextHolder;
import com.hssy.framework.commom.constant.GlobalConstants;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/17 19:25
 */
@Slf4j
public class FeignRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        Long userId = LoginUserContextHolder.getUserId();
        if (Objects.nonNull(userId)){
            requestTemplate.header(GlobalConstants.USER_ID,String.valueOf(userId));
            log.info("将用户id设置到Feign请求头中，userId:{}",userId);
        }
    }
}

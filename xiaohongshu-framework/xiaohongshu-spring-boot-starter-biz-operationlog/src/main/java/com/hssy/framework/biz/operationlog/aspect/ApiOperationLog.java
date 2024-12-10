package com.hssy.framework.biz.operationlog.aspect;

import java.lang.annotation.*;

/**
 * @author 13759
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface ApiOperationLog {
    /**
     * API 功能描述
     *
     * @return
     */
    String description() default "";

}
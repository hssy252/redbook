package com.hssy.xiaohongshu.user.biz.enums;

import com.hssy.framework.commom.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author 13759
 */

@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("USER-10000", "出错啦，后台小哥正在努力修复中..."),
    PARAM_NOT_VALID("USER-10001", "参数错误"),

    // ----------- 业务异常状态码 -----------
    ;

    // 异常码
    private final String errorCode;
    // 错误信息
    private final String errorMessage;

}
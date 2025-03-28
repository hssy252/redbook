package com.hssy.xiaohongshu.user.relation.biz.enums;

import com.hssy.framework.commom.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("RELATION-10000", "出错啦，后台小哥正在努力修复中..."),
    PARAM_NOT_VALID("RELATION-10001", "参数错误"),

    // ----------- 业务异常状态码 -----------
    CANT_FOLLOW_YOURSELF("RELATION-20000","您不能关注您自己"),
    FOLLOW_USER_NOT_EXIST("RELATION-20001","被关注的用户不存在"),
    FOLLOW_USER_EXCEED_MAX_COUNT("RELATION-20002","关注的用户超过上限"),
    ALREADY_FOLLOWED("RELATION-20003", "您已经关注了该用户"),
    CANT_UNFOLLOW_YOUR_SELF("RELATION-20005", "无法取关自己"),
    NOT_FOLLOWED("RELATION-20006", "你未关注对方，无法取关"),
    ;

    // 异常码
    private final String errorCode;
    // 错误信息
    private final String errorMessage;

}
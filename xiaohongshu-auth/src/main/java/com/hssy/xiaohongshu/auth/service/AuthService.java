package com.hssy.xiaohongshu.auth.service;

import com.hssy.framework.commom.response.Response;
import com.hssy.xiaohongshu.auth.model.vo.user.UpdatePasswordReqVO;
import com.hssy.xiaohongshu.auth.model.vo.user.UserLoginReqVO;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/11 21:06
 */
public interface AuthService {

    /**
     * 登录与注册
     * @param userLoginReqVO
     * @return
     */
    Response<String> loginAndRegister(UserLoginReqVO userLoginReqVO);

    /**
     * 退出登录
     * @return
     */
    Response<?> logout(Long userId);


    Response<?> updatePassword(UpdatePasswordReqVO updatePasswordReqVO);
}

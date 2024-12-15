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
public interface UserService {

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

    /**
     * 修改用户密码
     * @param updatePasswordReqVO
     * @return
     */
    Response<?> updatePassword(UpdatePasswordReqVO updatePasswordReqVO);
}

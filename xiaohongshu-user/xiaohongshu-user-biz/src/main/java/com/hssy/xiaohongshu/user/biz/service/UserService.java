package com.hssy.xiaohongshu.user.biz.service;

import com.hssy.framework.commom.response.Response;
import com.hssy.xiaohongshu.user.biz.model.vo.UpdateUserInfoReqVO;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/16 21:52
 */
public interface UserService {

    /**
     * 更新用户信息
     * @param updateUserInfoReqVO
     * @return
     */
    Response<?> updateUserInfo(UpdateUserInfoReqVO updateUserInfoReqVO);

}

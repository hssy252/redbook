package com.hssy.xiaohongshu.user.biz.service;

import com.hssy.framework.commom.response.Response;
import com.hssy.xiaohongshu.user.api.dto.req.FindUserByIdReqDTO;
import com.hssy.xiaohongshu.user.api.dto.req.FindUserByPhoneReqDTO;
import com.hssy.xiaohongshu.user.api.dto.req.RegisterUserReqDTO;
import com.hssy.xiaohongshu.user.api.dto.req.UpdateUserPasswordReqDTO;
import com.hssy.xiaohongshu.user.api.dto.resp.FindUserByIdRspDTO;
import com.hssy.xiaohongshu.user.api.dto.resp.FindUserByPhoneRspDTO;
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

    /**
     * 用户注册
     *
     * @param registerUserReqDTO
     * @return
     */
    Response<Long> register(RegisterUserReqDTO registerUserReqDTO);

    /**
     * 根据手机号查询用户信息
     *
     * @param findUserByPhoneReqDTO
     * @return
     */
    Response<FindUserByPhoneRspDTO> findByPhone(FindUserByPhoneReqDTO findUserByPhoneReqDTO);

    /**
     * 更新密码
     *
     * @param updateUserPasswordReqDTO
     * @return
     */
    Response<?> updatePassword(UpdateUserPasswordReqDTO updateUserPasswordReqDTO);

    /**
     * 根据id返回用户信息
     * @param findUserByIdReqDTO
     * @return
     */
    Response<FindUserByIdRspDTO> findById(FindUserByIdReqDTO findUserByIdReqDTO);
}

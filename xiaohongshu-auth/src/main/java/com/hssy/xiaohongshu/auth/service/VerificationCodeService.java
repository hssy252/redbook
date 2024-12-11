package com.hssy.xiaohongshu.auth.service;

import com.hssy.framework.commom.response.Response;
import com.hssy.xiaohongshu.auth.model.vo.verificationcode.SendVerificationCodeReqVO;

/**
 * @author 13759
 */
public interface VerificationCodeService {

    /**
     * 发送短信验证码
     *
     * @param sendVerificationCodeReqVO
     * @return
     */
    Response<?> send(SendVerificationCodeReqVO sendVerificationCodeReqVO);
}
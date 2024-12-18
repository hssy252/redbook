package com.hssy.xiaohongshu.auth.controller;

import com.hssy.framework.biz.operationlog.aspect.ApiOperationLog;
import com.hssy.framework.commom.response.Response;
import com.hssy.xiaohongshu.auth.filter.LoginUserContextHolder;
import com.hssy.xiaohongshu.auth.model.vo.user.UpdatePasswordReqVO;
import com.hssy.xiaohongshu.auth.model.vo.user.UserLoginReqVO;
import com.hssy.xiaohongshu.auth.service.AuthService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/11 21:17
 */
@RestController
@Slf4j
public class AuthController {

    @Resource
    private AuthService authService;

    @PostMapping("/login")
    @ApiOperationLog(description = "用户登录/注册")
    public Response<String> loginAndRegister(@Validated @RequestBody UserLoginReqVO userLoginReqVO) {
        return authService.loginAndRegister(userLoginReqVO);
    }

    @PostMapping("/logout")
    @ApiOperationLog(description = "账号登出")
    public Response<?> logout() {
        Long userId = LoginUserContextHolder.getUserId();
        return authService.logout(userId);
    }

    @PostMapping("/password/update")
    @ApiOperationLog(description = "更改密码")
    public Response<?> updatePassword(@RequestBody @Validated UpdatePasswordReqVO updatePasswordReqVO){
        return authService.updatePassword(updatePasswordReqVO);
    }

}

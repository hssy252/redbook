package com.hssy.xiaohongshu.user.biz.controller;

import com.hssy.framework.biz.operationlog.aspect.ApiOperationLog;
import com.hssy.framework.commom.response.Response;
import com.hssy.xiaohongshu.user.api.dto.req.FindUserByIdReqDTO;
import com.hssy.xiaohongshu.user.api.dto.req.FindUserByPhoneReqDTO;
import com.hssy.xiaohongshu.user.api.dto.req.RegisterUserReqDTO;
import com.hssy.xiaohongshu.user.api.dto.req.UpdateUserPasswordReqDTO;
import com.hssy.xiaohongshu.user.api.dto.resp.FindUserByIdRspDTO;
import com.hssy.xiaohongshu.user.api.dto.resp.FindUserByPhoneRspDTO;
import com.hssy.xiaohongshu.user.biz.model.vo.UpdateUserInfoReqVO;
import com.hssy.xiaohongshu.user.biz.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/16 21:32
 */

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户信息修改
     *
     * @param updateUserInfoReqVO
     * @return
     */
    @PostMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<?> updateUserInfo(@Validated UpdateUserInfoReqVO updateUserInfoReqVO) {
        return userService.updateUserInfo(updateUserInfoReqVO);
    }

    // ===================================== 对其他服务提供的接口 =====================================
    @PostMapping("/register")
    @ApiOperationLog(description = "用户注册")
    public Response<Long> register(@Validated @RequestBody RegisterUserReqDTO registerUserReqDTO) {
        return userService.register(registerUserReqDTO);
    }

    @PostMapping("/findByPhone")
    @ApiOperationLog(description = "手机号查询用户信息")
    public Response<FindUserByPhoneRspDTO> findByPhone(@Validated @RequestBody FindUserByPhoneReqDTO findUserByPhoneReqDTO) {
        return userService.findByPhone(findUserByPhoneReqDTO);
    }

    @PostMapping("/password/update")
    @ApiOperationLog(description = "密码更新")
    public Response<?> updatePassword(@Validated @RequestBody UpdateUserPasswordReqDTO updateUserPasswordReqDTO) {
        return userService.updatePassword(updateUserPasswordReqDTO);
    }

    @PostMapping("/findById")
    @ApiOperationLog(description = "根据id查询用户")
    public Response<FindUserByIdRspDTO> findUserById(@Validated @RequestBody FindUserByIdReqDTO findUserByIdReqDTO){
        return userService.findById(findUserByIdReqDTO);
    }


}

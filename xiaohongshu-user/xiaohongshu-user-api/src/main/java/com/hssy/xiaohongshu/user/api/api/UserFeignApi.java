package com.hssy.xiaohongshu.user.api.api;

import com.hssy.framework.commom.response.Response;
import com.hssy.xiaohongshu.user.api.constants.ApiConstants;
import com.hssy.xiaohongshu.user.api.dto.req.FindUserByIdReqDTO;
import com.hssy.xiaohongshu.user.api.dto.req.FindUserByPhoneReqDTO;
import com.hssy.xiaohongshu.user.api.dto.req.FindUsersByIdsReqDTO;
import com.hssy.xiaohongshu.user.api.dto.req.RegisterUserReqDTO;
import com.hssy.xiaohongshu.user.api.dto.req.UpdateUserPasswordReqDTO;
import com.hssy.xiaohongshu.user.api.dto.req.UserExistReqDTO;
import com.hssy.xiaohongshu.user.api.dto.resp.FindUserByIdRspDTO;
import com.hssy.xiaohongshu.user.api.dto.resp.FindUserByPhoneRspDTO;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/18 10:54
 */
@FeignClient(name = ApiConstants.SERVICE_NAME)
public interface UserFeignApi {

    String PREFIX = "/user";

    /**
     * 用户注册
     *
     * @param registerUserReqDTO
     * @return
     */
    @PostMapping(value = PREFIX + "/register")
    Response<Long> registerUser(@RequestBody RegisterUserReqDTO registerUserReqDTO);

    /**
     * 根据手机号查询用户信息
     *
     * @param findUserByPhoneReqDTO
     * @return
     */
    @PostMapping(value = PREFIX + "/findByPhone")
    Response<FindUserByPhoneRspDTO> findByPhone(@RequestBody FindUserByPhoneReqDTO findUserByPhoneReqDTO);


    /**
     * 更新密码
     *
     * @param updateUserPasswordReqDTO
     * @return
     */
    @PostMapping(value = PREFIX + "/password/update")
    Response<?> updatePassword(@RequestBody UpdateUserPasswordReqDTO updateUserPasswordReqDTO);

    @PostMapping(value = PREFIX + "/findById")
    Response<FindUserByIdRspDTO> findUserById(@RequestBody FindUserByIdReqDTO findUserByIdReqDTO);

    @PostMapping(value = PREFIX + "/exist")
    Response<Boolean> userExistOrNot(@RequestBody UserExistReqDTO userExistReqDTO);

    /**
     * 批量查询用户信息
     *
     * @param findUsersByIdsReqDTO
     * @return
     */
    @PostMapping(value = PREFIX + "/findByIds")
    Response<List<FindUserByIdRspDTO>> findByIds(@RequestBody FindUsersByIdsReqDTO findUsersByIdsReqDTO);

}

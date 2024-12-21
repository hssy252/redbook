package com.hssy.xiaohongshu.note.biz.rpc;

import com.hssy.framework.commom.response.Response;
import com.hssy.xiaohongshu.user.api.api.UserFeignApi;
import com.hssy.xiaohongshu.user.api.dto.req.FindUserByIdReqDTO;
import com.hssy.xiaohongshu.user.api.dto.resp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/21 16:40
 */

@Component
public class UserRpcService {

    @Resource
    private UserFeignApi userFeignApi;

    /**
     * 根据id查询用户信息
     * @param userId
     * @return
     */
    public FindUserByIdRspDTO findUserInfoById(Long userId){
        FindUserByIdReqDTO reqDTO = FindUserByIdReqDTO.builder()
            .id(userId)
            .build();

        Response<FindUserByIdRspDTO> userInfo = userFeignApi.findUserById(reqDTO);
        if (userInfo==null || !userInfo.isSuccess()){
            return null;
        }
        return userInfo.getData();
    }

}

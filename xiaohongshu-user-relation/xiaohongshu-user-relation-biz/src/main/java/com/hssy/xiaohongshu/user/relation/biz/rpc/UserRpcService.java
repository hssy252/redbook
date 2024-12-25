package com.hssy.xiaohongshu.user.relation.biz.rpc;

import cn.hutool.core.collection.CollUtil;
import com.hssy.framework.commom.response.Response;
import com.hssy.xiaohongshu.user.api.api.UserFeignApi;
import com.hssy.xiaohongshu.user.api.dto.req.FindUserByIdReqDTO;
import com.hssy.xiaohongshu.user.api.dto.req.FindUsersByIdsReqDTO;
import com.hssy.xiaohongshu.user.api.dto.req.UserExistReqDTO;
import com.hssy.xiaohongshu.user.api.dto.resp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/22 21:13
 */
@Component
public class UserRpcService {

    @Resource
    private UserFeignApi userFeignApi;

    public Boolean userExistOrNot(Long userId){
        FindUserByIdReqDTO reqDTO = new FindUserByIdReqDTO();
        reqDTO.setId(userId);

        Response<FindUserByIdRspDTO> response = userFeignApi.findUserById(reqDTO);
        if (Objects.isNull(response)||!response.isSuccess()){
            return null;
        }

        return Boolean.TRUE;
    }

    /**
     * 根据id批量查询用户信息
     * @param userIds
     * @return
     */
    public List<FindUserByIdRspDTO> findByIds(List<Long> userIds){
        FindUsersByIdsReqDTO findUsersByIdsReqDTO = new FindUsersByIdsReqDTO();
        findUsersByIdsReqDTO.setIds(userIds);

        Response<List<FindUserByIdRspDTO>> response = userFeignApi.findByIds(findUsersByIdsReqDTO);

        if (!response.isSuccess() || Objects.isNull(response.getData()) || CollUtil.isEmpty(response.getData())) {
            return null;
        }

        return response.getData();

    }

}

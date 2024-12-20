package com.hssy.xiaohongshu.user.biz.domain.mapper;

import com.hssy.xiaohongshu.user.api.dto.resp.FindUserByIdRspDTO;
import com.hssy.xiaohongshu.user.biz.domain.dataobject.UserDO;

public interface UserDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(UserDO record);

    int insertSelective(UserDO record);

    UserDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(UserDO record);

    int updateByPrimaryKey(UserDO record);

    /**
     * 根据手机号查询记录
     * @param phone
     * @return
     */
    UserDO selectByPhone(String phone);

    FindUserByIdRspDTO findById(Long id);
}
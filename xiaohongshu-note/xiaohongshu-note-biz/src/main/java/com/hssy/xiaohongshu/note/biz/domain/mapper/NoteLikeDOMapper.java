package com.hssy.xiaohongshu.note.biz.domain.mapper;

import com.hssy.xiaohongshu.note.biz.domain.dataobject.NoteLikeDO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface NoteLikeDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(NoteLikeDO record);

    int insertSelective(NoteLikeDO record);

    NoteLikeDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(NoteLikeDO record);

    int updateByPrimaryKey(NoteLikeDO record);

    int selectCountByUserIdAndNoteId(@Param("userId") Long userId, @Param("noteId") Long noteId);

    List<NoteLikeDO> selectByUserId(@Param("userId") Long userId);


}
package com.hssy.xiaohongshu.note.biz.service;

import com.hssy.framework.commom.response.Response;
import com.hssy.xiaohongshu.note.biz.model.vo.FindNoteDetailReqVO;
import com.hssy.xiaohongshu.note.biz.model.vo.FindNoteDetailRspVO;
import com.hssy.xiaohongshu.note.biz.model.vo.PublishNoteReqVO;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/20 16:07
 */
public interface NoteService {

    /**
     * 发布笔记
     * @param publishNoteReqVO
     * @return
     */
    Response<?> publishNote(PublishNoteReqVO publishNoteReqVO);

    /**
     * 笔记详情
     * @param findNoteDetailReqVO
     * @return
     */
    Response<FindNoteDetailRspVO> findNoteDetail(FindNoteDetailReqVO findNoteDetailReqVO);

}

package com.hssy.xiaohongshu.note.biz.service;

import com.hssy.framework.commom.response.Response;
import com.hssy.xiaohongshu.note.biz.model.vo.DeleteNoteReqVO;
import com.hssy.xiaohongshu.note.biz.model.vo.FindNoteDetailReqVO;
import com.hssy.xiaohongshu.note.biz.model.vo.FindNoteDetailRspVO;
import com.hssy.xiaohongshu.note.biz.model.vo.PublishNoteReqVO;
import com.hssy.xiaohongshu.note.biz.model.vo.TopNoteReqVO;
import com.hssy.xiaohongshu.note.biz.model.vo.UpdateNoteReqVO;
import com.hssy.xiaohongshu.note.biz.model.vo.UpdateNoteVisibleOnlyMeReqVO;

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


    /**
     * 笔记更新
     * @param updateNoteReqVO
     * @return
     */
    Response<?> updateNote(UpdateNoteReqVO updateNoteReqVO);

    /**
     * 删除本地笔记缓存
     * @param noteId
     */
    void deleteNoteLocalCache(Long noteId);

    Response<?> deleteNote(DeleteNoteReqVO deleteNoteReqVO);

    /**
     * 笔记仅对自己可见
     * @param updateNoteVisibleOnlyMeReqVO
     * @return
     */
    Response<?> visibleOnlyMe(UpdateNoteVisibleOnlyMeReqVO updateNoteVisibleOnlyMeReqVO);

    /**
     * 笔记置顶 / 取消置顶
     * @param topNoteReqVO
     * @return
     */
    Response<?> topNote(TopNoteReqVO topNoteReqVO);

}

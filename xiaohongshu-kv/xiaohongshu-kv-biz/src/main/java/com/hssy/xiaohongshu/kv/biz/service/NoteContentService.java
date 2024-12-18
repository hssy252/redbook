package com.hssy.xiaohongshu.kv.biz.service;

import com.hssy.framework.commom.response.Response;
import com.hssy.xiaohongshu.kv.dto.req.AddNoteContentReqDTO;
import com.hssy.xiaohongshu.kv.dto.req.FindNoteContentReqDTO;
import com.hssy.xiaohongshu.kv.dto.resp.FindNoteContentRespDTO;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/18 16:49
 */
public interface NoteContentService {

   /**
    * 添加笔记内容
    * @param addNoteContentReqDTO
    * @return
    */
   Response<?> addNoteContent(AddNoteContentReqDTO addNoteContentReqDTO);

   /**
    * 查询笔记内容
    * @param findNoteContentReqDTO
    * @return
    */
   Response<FindNoteContentRespDTO> findNoteContent(FindNoteContentReqDTO findNoteContentReqDTO);

}

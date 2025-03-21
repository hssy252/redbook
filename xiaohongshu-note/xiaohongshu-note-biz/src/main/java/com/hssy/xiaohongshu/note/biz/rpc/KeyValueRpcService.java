package com.hssy.xiaohongshu.note.biz.rpc;

import com.hssy.framework.commom.response.Response;
import com.hssy.xiaohongshu.kv.api.KeyValueFeignApi;
import com.hssy.xiaohongshu.kv.dto.req.AddNoteContentReqDTO;
import com.hssy.xiaohongshu.kv.dto.req.DeleteNoteContentReqDTO;
import com.hssy.xiaohongshu.kv.dto.req.FindNoteContentReqDTO;
import com.hssy.xiaohongshu.kv.dto.resp.FindNoteContentRespDTO;
import com.hssy.xiaohongshu.note.biz.domain.dataobject.NoteDO;
import jakarta.annotation.Resource;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/20 15:59
 */
@Component
public class KeyValueRpcService {

    @Resource
    private KeyValueFeignApi keyValueFeignApi;

    /**
     * 保存笔记内容
     *
     * @param uuid
     * @param content
     * @return
     */
    public boolean saveNoteContent(String uuid, String content) {
        AddNoteContentReqDTO addNoteContentReqDTO = new AddNoteContentReqDTO();
        addNoteContentReqDTO.setUuid(uuid);
        addNoteContentReqDTO.setContent(content);

        Response<?> response = keyValueFeignApi.addNoteContent(addNoteContentReqDTO);

        if (Objects.isNull(response) || !response.isSuccess()) {
            return false;
        }

        return true;
    }

    /**
     * 删除笔记内容
     *
     * @param uuid
     * @return
     */
    public boolean deleteNoteContent(String uuid) {
        DeleteNoteContentReqDTO deleteNoteContentReqDTO = new DeleteNoteContentReqDTO();
        deleteNoteContentReqDTO.setUuid(uuid);

        Response<?> response = keyValueFeignApi.deleteNoteContent(deleteNoteContentReqDTO);

        if (Objects.isNull(response) || !response.isSuccess()) {
            return false;
        }

        return true;
    }

    /**
     * 根据笔记内容的uuid查询笔记内容
     * @param noteId
     * @return
     */
    public String findNoteContentById(String noteId){
        FindNoteContentReqDTO reqDTO = FindNoteContentReqDTO.builder()
            .uuid(noteId)
            .build();

        Response<FindNoteContentRespDTO> response = keyValueFeignApi.findNoteContent(reqDTO);
        if (Objects.isNull(response)||!response.isSuccess()){
            return null;
        }
        return response.getData().getContent();
    }

}

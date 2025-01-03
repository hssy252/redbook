package com.hssy.xiaohongshu.kv.biz.service.impl;

import com.hssy.framework.commom.exception.BizException;
import com.hssy.framework.commom.response.Response;
import com.hssy.xiaohongshu.kv.biz.domain.dataobject.NoteContentDO;
import com.hssy.xiaohongshu.kv.biz.domain.repository.NoteContentRepository;
import com.hssy.xiaohongshu.kv.biz.enums.ResponseCodeEnum;
import com.hssy.xiaohongshu.kv.biz.service.NoteContentService;
import com.hssy.xiaohongshu.kv.dto.req.AddNoteContentReqDTO;
import com.hssy.xiaohongshu.kv.dto.req.DeleteNoteContentReqDTO;
import com.hssy.xiaohongshu.kv.dto.req.FindNoteContentReqDTO;
import com.hssy.xiaohongshu.kv.dto.resp.FindNoteContentRespDTO;
import jakarta.annotation.Resource;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/18 16:50
 */

@Service
public class NoteContentServiceImpl implements NoteContentService {

    @Resource
    private NoteContentRepository noteContentRepository;

    @Override
    public Response<?> addNoteContent(AddNoteContentReqDTO addNoteContentReqDTO) {
        String uuid = addNoteContentReqDTO.getUuid();
        String content = addNoteContentReqDTO.getContent();

        NoteContentDO noteContentDO = NoteContentDO.builder()
            .id(UUID.fromString(uuid))
            .content(content)
            .build();

        noteContentRepository.save(noteContentDO);
        return Response.success();
    }

    @Override
    public Response<FindNoteContentRespDTO> findNoteContent(FindNoteContentReqDTO findNoteContentReqDTO) {
        String noteId = findNoteContentReqDTO.getUuid();
        Optional<NoteContentDO> note = noteContentRepository.findById(UUID.fromString(noteId));

        if (!note.isPresent()){
            throw new BizException(ResponseCodeEnum.NOTE_CONTENT_NOT_FOUND);
        }

        NoteContentDO noteContentDO = note.get();

        FindNoteContentRespDTO respDTO = FindNoteContentRespDTO.builder()
            .uuid(noteContentDO.getId())
            .content(noteContentDO.getContent())
            .build();

        return Response.success(respDTO);
    }

    @Override
    public Response<?> deleteNoteContent(DeleteNoteContentReqDTO deleteNoteContentReqDTO) {
        String noteId = deleteNoteContentReqDTO.getUuid();
        noteContentRepository.deleteById(UUID.fromString(noteId));
        return Response.success();
    }
}

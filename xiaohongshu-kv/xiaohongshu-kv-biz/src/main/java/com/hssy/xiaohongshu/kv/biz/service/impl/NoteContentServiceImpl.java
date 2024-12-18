package com.hssy.xiaohongshu.kv.biz.service.impl;

import com.hssy.framework.commom.response.Response;
import com.hssy.xiaohongshu.kv.biz.domain.dataobject.NoteContentDO;
import com.hssy.xiaohongshu.kv.biz.domain.repository.NoteContentRepository;
import com.hssy.xiaohongshu.kv.biz.service.NoteContentService;
import com.hssy.xiaohongshu.kv.dto.req.AddNoteContentReqDTO;
import jakarta.annotation.Resource;
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
        Long id = addNoteContentReqDTO.getId();
        String content = addNoteContentReqDTO.getContent();

        NoteContentDO noteContentDO = NoteContentDO.builder()
            .id(UUID.randomUUID()) // todo 暂时不用真实id
            .content(content)
            .build();

        noteContentRepository.save(noteContentDO);
        return Response.success();
    }
}

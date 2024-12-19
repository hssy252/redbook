package com.hssy.xiaohongshu.kv.biz.controller;

import com.hssy.framework.commom.response.Response;
import com.hssy.xiaohongshu.kv.biz.service.NoteContentService;
import com.hssy.xiaohongshu.kv.dto.req.AddNoteContentReqDTO;
import com.hssy.xiaohongshu.kv.dto.req.DeleteNoteContentReqDTO;
import com.hssy.xiaohongshu.kv.dto.req.FindNoteContentReqDTO;
import com.hssy.xiaohongshu.kv.dto.resp.FindNoteContentRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/18 16:53
 */

@RestController
@RequestMapping("/kv")
@Slf4j
public class NoteContentController {

    @Resource
    private NoteContentService noteContentService;

    @PostMapping("/note/content/add")
    public Response<?> addNoteContent(@RequestBody @Validated AddNoteContentReqDTO addNoteContentReqDTO){
        return noteContentService.addNoteContent(addNoteContentReqDTO);
    }

    @PostMapping(value = "/note/content/find")
    public Response<FindNoteContentRespDTO> findNoteContent(@Validated @RequestBody FindNoteContentReqDTO findNoteContentReqDTO) {
        return noteContentService.findNoteContent(findNoteContentReqDTO);
    }

    @PostMapping("/note/content/find")
    public Response<?> deleteNoteContent(@RequestBody @Validated DeleteNoteContentReqDTO deleteNoteContentReqDTO){
        return noteContentService.deleteNoteContent(deleteNoteContentReqDTO);
    }

}

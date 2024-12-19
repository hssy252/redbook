package com.hssy.xiaohongshu.kv.api;

import com.hssy.framework.commom.response.Response;
import com.hssy.xiaohongshu.kv.constants.ApiConstants;
import com.hssy.xiaohongshu.kv.dto.req.AddNoteContentReqDTO;
import com.hssy.xiaohongshu.kv.dto.req.DeleteNoteContentReqDTO;
import com.hssy.xiaohongshu.kv.dto.req.FindNoteContentReqDTO;
import com.hssy.xiaohongshu.kv.dto.resp.FindNoteContentRespDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/18 16:59
 */
@FeignClient(name = ApiConstants.SERVICE_NAME)
public interface KeyValueFeignApi {

    String PREFIX = "/kv";

    @PostMapping(PREFIX+"/note/content/add")
    Response<?> addNoteContent(@RequestBody AddNoteContentReqDTO addNoteContentReqDTO);

    @PostMapping(value = PREFIX + "/note/content/find")
    Response<FindNoteContentRespDTO> findNoteContent(@RequestBody FindNoteContentReqDTO findNoteContentReqDTO);

    @PostMapping(PREFIX+"/note/content/delete")
    Response<?> deleteNoteContent(@RequestBody DeleteNoteContentReqDTO dto);


}

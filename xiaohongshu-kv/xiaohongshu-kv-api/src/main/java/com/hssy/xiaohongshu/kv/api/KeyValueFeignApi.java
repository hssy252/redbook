package com.hssy.xiaohongshu.kv.api;

import com.hssy.framework.commom.response.Response;
import com.hssy.xiaohongshu.kv.constants.ApiConstants;
import com.hssy.xiaohongshu.kv.dto.req.AddNoteContentReqDTO;
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

    String PRIFIX = "kv";

    @PostMapping(PRIFIX+"/note/content/add")
    Response<?> addNoteContent(@RequestBody AddNoteContentReqDTO addNoteContentReqDTO);

}

package com.hssy.xiaohongshu.kv.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/19 10:06
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class DeleteNoteContentReqDTO {

    @NotBlank(message = "笔记id不能为空")
    private String noteId;

}

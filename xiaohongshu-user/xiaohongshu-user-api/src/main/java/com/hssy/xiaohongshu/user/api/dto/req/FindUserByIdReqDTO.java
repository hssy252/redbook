package com.hssy.xiaohongshu.user.api.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/20 19:45
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindUserByIdReqDTO {

    /**
     * 用户id
     */
    @NotNull(message = "用户id不能为空")
    private Long id;

}

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
 * @since 2024/12/22 19:58
 */

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UserExistReqDTO {

    @NotNull(message = "用户id不能为空")
    private Long userId;

}

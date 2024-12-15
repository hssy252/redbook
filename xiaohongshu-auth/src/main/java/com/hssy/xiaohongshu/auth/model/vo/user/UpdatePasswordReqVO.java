package com.hssy.xiaohongshu.auth.model.vo.user;

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
 * @since 2024/12/15 16:20
 */

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UpdatePasswordReqVO {

    @NotBlank(message = "新密码不能为空")
    private String newPassword;

}

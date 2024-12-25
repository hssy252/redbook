package com.hssy.xiaohongshu.user.relation.biz.model.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/25 19:18
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindFansUserReqVO {

    /**
     * 要查询那个用户的粉丝列表
     */
    @NotNull(message = "用户id不能为空")
    private Long userId;

    /**
     * 查询页码（默认为1）
     */
    @NotNull(message = "页码不能为空")
    private Long pageNo = 1L;

}

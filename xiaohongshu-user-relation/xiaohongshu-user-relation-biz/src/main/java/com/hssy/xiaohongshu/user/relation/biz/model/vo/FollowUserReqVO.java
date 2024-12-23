package com.hssy.xiaohongshu.user.relation.biz.model.vo;

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
 * @since 2024/12/22 18:51
 */

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class FollowUserReqVO {

    /**
     * 被关注的用户的id
     */
    @NotNull(message = "被关注用户的id不能为空")
    private Long followUserId;

}

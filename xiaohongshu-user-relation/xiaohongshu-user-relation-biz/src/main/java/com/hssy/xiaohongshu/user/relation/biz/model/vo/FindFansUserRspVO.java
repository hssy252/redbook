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
 * @since 2024/12/25 19:20
 */

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class FindFansUserRspVO {

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 图片
     */
    private String avatar;

    /**
     * 粉丝总数
     */
    private Long fansTotal;

    /**
     * 笔记总数
     */
    private Long noteTotal;

    /**
     * 昵称
     */
    private String nickname;

}

package com.hssy.xiaohongshu.count.biz.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author: 犬小哈
 * @date: 2024/4/7 15:17
 * @version: v1.0.0
 * @description: 点赞、取消点赞笔记
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CountLikeUnlikeNoteMqDTO {

    private Long userId;

    private Long noteId;

    /**
     * 0: 取消点赞， 1：点赞
     */
    private Integer type;

    private LocalDateTime createTime;

    /**
     * 笔记发布者 ID
     */
    private Long noteCreatorId;
}


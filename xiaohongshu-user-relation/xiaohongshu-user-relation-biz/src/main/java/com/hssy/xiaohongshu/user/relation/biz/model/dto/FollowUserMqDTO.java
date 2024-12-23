package com.hssy.xiaohongshu.user.relation.biz.model.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/23 16:32
 */

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class FollowUserMqDTO {

    private Long userId;

    private Long followUserId;

    private LocalDateTime createTime;

}

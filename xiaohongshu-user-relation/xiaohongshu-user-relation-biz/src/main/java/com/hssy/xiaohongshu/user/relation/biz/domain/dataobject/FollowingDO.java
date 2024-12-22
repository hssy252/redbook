package com.hssy.xiaohongshu.user.relation.biz.domain.dataobject;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class FollowingDO {

    private Long id;

    private Long userId;

    private Long followingUserId;

    private LocalDateTime createTime;

}
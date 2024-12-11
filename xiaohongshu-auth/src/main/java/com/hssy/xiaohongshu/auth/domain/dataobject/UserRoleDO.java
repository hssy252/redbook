package com.hssy.xiaohongshu.auth.domain.dataobject;

import java.time.LocalDateTime;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 13759
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserRoleDO {
    private Long id;

    private Long userId;

    private Long roleId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Boolean isDeleted;

}
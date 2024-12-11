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
public class RoleDO {
    private Long id;

    private String roleName;

    private String roleKey;

    private Byte status;

    private Integer sort;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Boolean isDeleted;

}
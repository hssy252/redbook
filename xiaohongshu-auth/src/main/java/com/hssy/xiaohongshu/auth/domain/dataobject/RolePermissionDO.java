package com.hssy.xiaohongshu.auth.domain.dataobject;

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
public class RolePermissionDO {

    private Long id;

    private Long roleId;

    private Long permissionId;

    private Date createTime;

    private Date updateTime;

    private Boolean isDeleted;

}
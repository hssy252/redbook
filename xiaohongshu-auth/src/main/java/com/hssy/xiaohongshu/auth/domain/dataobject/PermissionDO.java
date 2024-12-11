package com.hssy.xiaohongshu.auth.domain.dataobject;

import java.time.LocalDateTime;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PermissionDO {
    private Long id;

    private Long parentId;

    private String name;

    private Byte type;

    private String menuUrl;

    private String menuIcon;

    private Integer sort;

    private String permissionKey;

    private Byte status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Boolean isDeleted;

}
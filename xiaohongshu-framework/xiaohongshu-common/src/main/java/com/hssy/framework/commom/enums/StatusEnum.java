package com.hssy.framework.commom.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author 13759
 */

@Getter
@AllArgsConstructor
public enum StatusEnum {
    // 启用
    ENABLE(0),
    // 禁用
    DISABLED(1);

    private final Integer value;
}
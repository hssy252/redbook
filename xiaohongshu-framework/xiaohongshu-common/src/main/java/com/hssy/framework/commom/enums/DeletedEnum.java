package com.hssy.framework.commom.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author 13759
 */

@Getter
@AllArgsConstructor
public enum DeletedEnum {

    YES(true),
    NO(false);

    private final Boolean value;
}
package com.hssy.framework.commom.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 功能简述
 *
 * @author hssy
 * @version 1.0
 * @since 2024/12/11 19:55
 */
public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {

    @Override
    public void initialize(PhoneNumber constraintAnnotation) {
        //进行初始化的操作
    }

    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext constraintValidatorContext) {
        return phoneNumber != null && phoneNumber.matches("\\d{11}");
    }
}

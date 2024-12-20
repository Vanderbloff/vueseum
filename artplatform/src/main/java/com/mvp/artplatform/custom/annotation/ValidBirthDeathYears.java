package com.mvp.artplatform.custom.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = BirthDeathYearValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBirthDeathYears {
    String message() default "Death year must be greater than birth year and within a reasonable range.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

package com.i2i.user_management.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;

public class NotFutureDateValidator implements ConstraintValidator<NotFutureDate, LocalDate> {

    @Override
    public boolean isValid(LocalDate date, ConstraintValidatorContext context) {

        if (date == null) {
            return true;
        }
        boolean isValid = !date.isAfter(LocalDate.now());
        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format("Invalid date '%s'! Expense date cannot be in the future. Current date: %s",
                            date, LocalDate.now())
            ).addConstraintViolation();
        }

        return isValid;
    }
}


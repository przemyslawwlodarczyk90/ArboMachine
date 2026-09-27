package com.example.arbomachine.domain.user.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginRequestDtoTest {

    private Validator beanValidator;

    @BeforeEach
    void createBeanValidator() {
        beanValidator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void malformedEmailIsRejected() {
        Set<ConstraintViolation<LoginRequestDto>> violations =
                beanValidator.validate(new LoginRequestDto("invalid-email", "password123"));

        assertEquals(1, violations.size());
        assertEquals("Email should be valid", violations.iterator().next().getMessage());
    }

    @Test
    void blankPasswordIsRejected() {
        Set<ConstraintViolation<LoginRequestDto>> violations =
                beanValidator.validate(new LoginRequestDto("test@example.com", ""));

        assertEquals(1, violations.size());
        assertEquals("Password is mandatory", violations.iterator().next().getMessage());
    }

    @Test
    void validLoginRequestPassesValidation() {
        assertTrue(beanValidator.validate(new LoginRequestDto("test@example.com", "password123")).isEmpty());
    }
}

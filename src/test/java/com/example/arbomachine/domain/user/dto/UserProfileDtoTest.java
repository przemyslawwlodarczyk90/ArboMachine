package com.example.arbomachine.domain.user.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserProfileDtoTest {

    private Validator beanValidator;

    @BeforeEach
    void createBeanValidator() {
        beanValidator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    /** Poprawny profil - kazdy test psuje w nim dokladnie jedno pole. */
    private UserProfileDto.UserProfileDtoBuilder validUserProfile() {
        return UserProfileDto.builder()
                .firstName("Jan")
                .lastName("Kowalski")
                .email("test@example.com")
                .phoneNumber("123456789")
                .street("Street")
                .postalCode("12-345")
                .city("City");
    }

    private String singleViolationMessage(UserProfileDto userProfile) {
        Set<ConstraintViolation<UserProfileDto>> violations = beanValidator.validate(userProfile);
        assertEquals(1, violations.size());
        return violations.iterator().next().getMessage();
    }

    @Test
    void blankFirstNameIsRejected() {
        assertEquals("First name is mandatory", singleViolationMessage(validUserProfile().firstName("").build()));
    }

    @Test
    void blankLastNameIsRejected() {
        assertEquals("Last name is mandatory", singleViolationMessage(validUserProfile().lastName("").build()));
    }

    @Test
    void malformedEmailIsRejected() {
        assertEquals("Email should be valid", singleViolationMessage(validUserProfile().email("invalid-email").build()));
    }

    @Test
    void malformedPhoneNumberIsRejected() {
        assertEquals("Phone number is invalid", singleViolationMessage(validUserProfile().phoneNumber("invalid-phone").build()));
    }

    @Test
    void postalCodeOutsidePolishFormatIsRejected() {
        assertEquals("Postal code is invalid", singleViolationMessage(validUserProfile().postalCode("invalid").build()));
    }

    @Test
    void validProfilePassesValidation() {
        assertTrue(beanValidator.validate(validUserProfile().build()).isEmpty());
    }
}

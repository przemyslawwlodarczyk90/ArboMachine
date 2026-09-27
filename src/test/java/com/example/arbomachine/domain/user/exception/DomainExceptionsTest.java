package com.example.arbomachine.domain.user.exception;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/** Wszystkie wyjatki domenowe musza byc niesprawdzane i przenosic komunikat - na nim opiera sie ApiExceptionHandler. */
class DomainExceptionsTest {

    static Stream<Arguments> domainExceptionFactories() {
        return Stream.of(
                Arguments.of("Account is locked", (Function<String, RuntimeException>) AccountLockedByAdminException::new),
                Arguments.of("Admin accounts can only be managed directly in the database",
                        (Function<String, RuntimeException>) AdminAccountModificationForbiddenException::new),
                Arguments.of("Email is already in use by an active user", (Function<String, RuntimeException>) EmailAlreadyInUseException::new),
                Arguments.of("User is not activated", (Function<String, RuntimeException>) EmailNotConfirmedException::new),
                Arguments.of("Invalid email or password", (Function<String, RuntimeException>) InvalidCredentialsException::new),
                Arguments.of("Token has expired", (Function<String, RuntimeException>) TokenExpiredException::new),
                Arguments.of("Token not found", (Function<String, RuntimeException>) TokenNotFoundException::new),
                Arguments.of("User not found", (Function<String, RuntimeException>) UserAccountNotFoundException::new)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("domainExceptionFactories")
    void domainExceptionIsUncheckedAndKeepsItsMessage(String expectedMessage,
                                                      Function<String, RuntimeException> exceptionFactory) {
        RuntimeException domainException = exceptionFactory.apply(expectedMessage);

        assertInstanceOf(RuntimeException.class, domainException);
        assertEquals(expectedMessage, domainException.getMessage());
    }
}

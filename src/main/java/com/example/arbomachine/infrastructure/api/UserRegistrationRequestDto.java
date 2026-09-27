package com.example.arbomachine.infrastructure.api;

import com.example.arbomachine.domain.user.dto.UserProfileDto;
import com.example.arbomachine.domain.user.enums.AssignableUserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Body POST /api/users/register. */
@Data
public class UserRegistrationRequestDto {

    @Valid
    @NotNull(message = "User data is mandatory")
    private UserProfileDto profile;

    @NotBlank(message = "Password is mandatory")
    @Size(min = 8, message = "Password must have at least 8 characters")
    private String password;

    /** ARBORIST albo OFFICIAL. Wartosc "ADMIN" nie przejdzie deserializacji - admina ustawia sie tylko w bazie. */
    @NotNull(message = "Account type is mandatory")
    private AssignableUserRole accountType;
}

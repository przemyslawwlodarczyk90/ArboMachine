package com.example.arbomachine.infrastructure.api;

import com.example.arbomachine.domain.user.dto.LoginRequestDto;
import com.example.arbomachine.domain.user.dto.LoginResponseDto;
import com.example.arbomachine.domain.user.dto.PasswordResetConfirmationDto;
import com.example.arbomachine.domain.user.dto.PasswordResetRequestDto;
import com.example.arbomachine.domain.user.dto.UserAccountDto;
import com.example.arbomachine.domain.user.dto.UserProfileDto;
import com.example.arbomachine.domain.user.facade.UserAccountFacade;
import com.example.arbomachine.domain.user.service.EmailConfirmationService;
import com.example.arbomachine.domain.user.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Publiczne endpointy kont (rejestracja, logowanie, potwierdzenie e-maila, reset hasla) oraz obsluga
 * WLASNEGO konta zalogowanego uzytkownika (/me). Operacje na cudzych kontach sa tylko w
 * {@link AdminUserAccountController}.
 */
@RestController
@RequestMapping("/api/users")
@Validated
@Tag(name = "Konta uzytkownikow", description = "Rejestracja, logowanie, reset hasla i wlasne konto")
public class UserAccountController {

    private final UserAccountFacade userAccountFacade;
    private final EmailConfirmationService emailConfirmationService;
    private final PasswordResetService passwordResetService;

    public UserAccountController(UserAccountFacade userAccountFacade,
                                 EmailConfirmationService emailConfirmationService,
                                 PasswordResetService passwordResetService) {
        this.userAccountFacade = userAccountFacade;
        this.emailConfirmationService = emailConfirmationService;
        this.passwordResetService = passwordResetService;
    }

    @Operation(summary = "Rejestracja nowego konta ARBORIST albo OFFICIAL (wysyla link aktywacyjny)")
    @PostMapping("/register")
    public ResponseEntity<String> registerNewUserAccount(@Valid @RequestBody UserRegistrationRequestDto registrationRequest) {
        userAccountFacade.registerNewUserAccount(registrationRequest.getProfile(), registrationRequest.getPassword(),
                registrationRequest.getAccountType());
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }

    @Operation(summary = "Logowanie - zwraca token JWT")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto loginRequest) {
        String accessToken = userAccountFacade.loginAndIssueAccessToken(loginRequest);
        return ResponseEntity.ok(new LoginResponseDto(accessToken));
    }

    @Operation(summary = "Potwierdzenie adresu e-mail tokenem z linku aktywacyjnego")
    @GetMapping("/confirm")
    public ResponseEntity<String> confirmEmail(@RequestParam String token) {
        emailConfirmationService.confirmEmailUsingToken(token);
        return ResponseEntity.ok("User confirmed successfully");
    }

    @Operation(summary = "Wyslanie linku do resetu hasla (zawsze 200 - nie zdradza, czy konto istnieje)")
    @PostMapping("/password-reset/request")
    public ResponseEntity<String> requestPasswordResetLink(@Valid @RequestBody PasswordResetRequestDto passwordResetRequest) {
        passwordResetService.sendPasswordResetLinkIfAccountExists(passwordResetRequest.email());
        return ResponseEntity.ok("If an account with this email exists, a reset link has been sent");
    }

    @Operation(summary = "Sprawdzenie, czy token resetu hasla jest jeszcze wazny")
    @GetMapping("/password-reset/validate")
    public ResponseEntity<String> validatePasswordResetToken(@RequestParam String token) {
        passwordResetService.ensurePasswordResetTokenIsValid(token);
        return ResponseEntity.ok("Token is valid");
    }

    @Operation(summary = "Ustawienie nowego hasla tokenem z linku")
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<String> resetPasswordUsingToken(@Valid @RequestBody PasswordResetConfirmationDto passwordResetConfirmation) {
        passwordResetService.resetPasswordUsingToken(passwordResetConfirmation.resetToken(),
                passwordResetConfirmation.newPassword());
        return ResponseEntity.ok("Password has been reset successfully");
    }

    @Operation(summary = "Konto zalogowanego uzytkownika")
    @GetMapping("/me")
    public ResponseEntity<UserAccountDto> getCurrentUserAccount(@AuthenticationPrincipal UserDetails authenticatedUser) {
        return ResponseEntity.ok(userAccountFacade.getCurrentUserAccount(authenticatedUser.getUsername()));
    }

    @Operation(summary = "Edycja wlasnego profilu")
    @PutMapping("/me")
    public ResponseEntity<UserAccountDto> updateCurrentUserProfile(@AuthenticationPrincipal UserDetails authenticatedUser,
                                                                   @Valid @RequestBody UserProfileDto updatedProfile) {
        return ResponseEntity.ok(userAccountFacade.updateCurrentUserProfile(authenticatedUser.getUsername(), updatedProfile));
    }

    @Operation(summary = "Usuniecie wlasnego konta")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteCurrentUserAccount(@AuthenticationPrincipal UserDetails authenticatedUser) {
        userAccountFacade.deleteCurrentUserAccount(authenticatedUser.getUsername());
        return ResponseEntity.noContent().build();
    }
}

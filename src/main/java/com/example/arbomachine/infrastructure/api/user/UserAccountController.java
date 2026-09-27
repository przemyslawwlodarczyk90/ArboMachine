package com.example.arbomachine.infrastructure.api.user;

import com.example.arbomachine.domain.user.dto.LoginRequestDto;
import com.example.arbomachine.domain.user.dto.LoginResponseDto;
import com.example.arbomachine.domain.user.dto.PasswordResetConfirmationDto;
import com.example.arbomachine.domain.user.dto.PasswordResetRequestDto;
import com.example.arbomachine.domain.user.dto.UserAccountDto;
import com.example.arbomachine.domain.user.dto.UserProfileDto;
import com.example.arbomachine.domain.user.facade.PasswordResetFacade;
import com.example.arbomachine.domain.user.facade.UserAccountFacade;
import com.example.arbomachine.domain.user.facade.UserAuthenticationFacade;
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
 * WLASNEGO konta zalogowanego uzytkownika (/me). Wola wylacznie fasady. Operacje na cudzych kontach sa tylko w
 * {@link com.example.arbomachine.infrastructure.api.admin.AdminUserAccountController}.
 */
@RestController
@RequestMapping("/api/users")
@Validated
@Tag(name = "Konta uzytkownikow", description = "Rejestracja, logowanie, reset hasla i wlasne konto")
public class UserAccountController {

    private final UserAuthenticationFacade userAuthenticationFacade;
    private final PasswordResetFacade passwordResetFacade;
    private final UserAccountFacade userAccountFacade;

    public UserAccountController(UserAuthenticationFacade userAuthenticationFacade,
                                 PasswordResetFacade passwordResetFacade,
                                 UserAccountFacade userAccountFacade) {
        this.userAuthenticationFacade = userAuthenticationFacade;
        this.passwordResetFacade = passwordResetFacade;
        this.userAccountFacade = userAccountFacade;
    }

    @Operation(summary = "Rejestracja nowego konta ARBORIST albo OFFICIAL (wysyla link aktywacyjny)")
    @PostMapping("/register")
    public ResponseEntity<String> registerNewUserAccount(@Valid @RequestBody UserRegistrationRequestDto registrationRequest) {
        userAuthenticationFacade.registerNewUserAccount(registrationRequest.getProfile(), registrationRequest.getPassword(),
                registrationRequest.getAccountType());
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }

    @Operation(summary = "Logowanie - zwraca token JWT")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto loginRequest) {
        String accessToken = userAuthenticationFacade.loginAndIssueAccessToken(loginRequest);
        return ResponseEntity.ok(new LoginResponseDto(accessToken));
    }

    @Operation(summary = "Potwierdzenie adresu e-mail tokenem z linku aktywacyjnego")
    @GetMapping("/confirm")
    public ResponseEntity<String> confirmEmail(@RequestParam String token) {
        userAuthenticationFacade.confirmEmailUsingToken(token);
        return ResponseEntity.ok("User confirmed successfully");
    }

    @Operation(summary = "Wyslanie linku do resetu hasla (zawsze 200 - nie zdradza, czy konto istnieje)")
    @PostMapping("/password-reset/request")
    public ResponseEntity<String> requestPasswordResetLink(@Valid @RequestBody PasswordResetRequestDto passwordResetRequest) {
        passwordResetFacade.sendPasswordResetLinkIfAccountExists(passwordResetRequest.email());
        return ResponseEntity.ok("If an account with this email exists, a reset link has been sent");
    }

    @Operation(summary = "Sprawdzenie, czy token resetu hasla jest jeszcze wazny")
    @GetMapping("/password-reset/validate")
    public ResponseEntity<String> validatePasswordResetToken(@RequestParam String token) {
        passwordResetFacade.ensurePasswordResetTokenIsValid(token);
        return ResponseEntity.ok("Token is valid");
    }

    @Operation(summary = "Ustawienie nowego hasla tokenem z linku")
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<String> resetPasswordUsingToken(@Valid @RequestBody PasswordResetConfirmationDto passwordResetConfirmation) {
        passwordResetFacade.resetPasswordUsingToken(passwordResetConfirmation.resetToken(),
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

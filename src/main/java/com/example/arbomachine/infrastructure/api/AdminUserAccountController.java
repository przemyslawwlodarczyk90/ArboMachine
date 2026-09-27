package com.example.arbomachine.infrastructure.api;

import com.example.arbomachine.domain.user.dto.ChangeAccountLockRequestDto;
import com.example.arbomachine.domain.user.dto.ChangeUserRoleRequestDto;
import com.example.arbomachine.domain.user.dto.UserAccountDto;
import com.example.arbomachine.domain.user.dto.UserAccountStatisticsDto;
import com.example.arbomachine.domain.user.dto.UserProfileDto;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.domain.user.service.AdminUserAccountManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Zarzadzanie cudzymi kontami - tylko ADMIN (wymuszone w SecurityConfig dla /api/admin/**). */
@RestController
@RequestMapping("/api/admin")
@Validated
@Tag(name = "Administracja", description = "Zarzadzanie kontami innych uzytkownikow (rola ADMIN)")
public class AdminUserAccountController {

    private final AdminUserAccountManagementService adminUserAccountManagementService;

    public AdminUserAccountController(AdminUserAccountManagementService adminUserAccountManagementService) {
        this.adminUserAccountManagementService = adminUserAccountManagementService;
    }

    @Operation(summary = "Statystyki kont")
    @GetMapping("/stats")
    public ResponseEntity<UserAccountStatisticsDto> getUserAccountStatistics() {
        return ResponseEntity.ok(adminUserAccountManagementService.getUserAccountStatistics());
    }

    @Operation(summary = "Lista kont, opcjonalnie filtrowana po roli")
    @GetMapping("/users")
    public ResponseEntity<List<UserAccountDto>> listUserAccounts(@RequestParam(name = "role", required = false) UserRole roleFilter) {
        return ResponseEntity.ok(adminUserAccountManagementService.listUserAccounts(roleFilter));
    }

    @Operation(summary = "Pojedyncze konto")
    @GetMapping("/users/{userAccountId}")
    public ResponseEntity<UserAccountDto> getUserAccount(@PathVariable Long userAccountId) {
        return ResponseEntity.ok(adminUserAccountManagementService.getUserAccountById(userAccountId));
    }

    @Operation(summary = "Edycja danych profilu konta")
    @PutMapping("/users/{userAccountId}")
    public ResponseEntity<UserAccountDto> updateUserProfile(@PathVariable Long userAccountId,
                                                            @Valid @RequestBody UserProfileDto updatedProfile) {
        return ResponseEntity.ok(adminUserAccountManagementService.updateUserProfileAsAdmin(userAccountId, updatedProfile));
    }

    @Operation(summary = "Zmiana roli konta (ARBORIST / OFFICIAL)")
    @PatchMapping("/users/{userAccountId}/role")
    public ResponseEntity<UserAccountDto> changeUserRole(@PathVariable Long userAccountId,
                                                         @Valid @RequestBody ChangeUserRoleRequestDto changeRoleRequest) {
        return ResponseEntity.ok(adminUserAccountManagementService.changeUserRole(userAccountId, changeRoleRequest.newRole()));
    }

    @Operation(summary = "Zablokowanie albo odblokowanie konta")
    @PatchMapping("/users/{userAccountId}/locked")
    public ResponseEntity<UserAccountDto> setAccountLockedByAdmin(@PathVariable Long userAccountId,
                                                                  @Valid @RequestBody ChangeAccountLockRequestDto changeLockRequest) {
        return ResponseEntity.ok(adminUserAccountManagementService.setAccountLockedByAdmin(userAccountId,
                changeLockRequest.lockedByAdmin()));
    }

    @Operation(summary = "Potwierdzenie e-maila w imieniu uzytkownika")
    @PostMapping("/users/{userAccountId}/confirm-email")
    public ResponseEntity<UserAccountDto> confirmEmailOnUsersBehalf(@PathVariable Long userAccountId) {
        return ResponseEntity.ok(adminUserAccountManagementService.confirmEmailOnUsersBehalf(userAccountId));
    }

    @Operation(summary = "Usuniecie konta")
    @DeleteMapping("/users/{userAccountId}")
    public ResponseEntity<Void> deleteUserAccount(@PathVariable Long userAccountId) {
        adminUserAccountManagementService.deleteUserAccountAsAdmin(userAccountId);
        return ResponseEntity.noContent().build();
    }
}

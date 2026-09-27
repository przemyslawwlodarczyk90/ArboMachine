package com.example.arbomachine.domain.user.service;

import com.example.arbomachine.domain.user.dto.UserAccountDto;
import com.example.arbomachine.domain.user.dto.UserAccountStatisticsDto;
import com.example.arbomachine.domain.user.dto.UserProfileDto;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.AssignableUserRole;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.domain.user.exception.AdminAccountModificationForbiddenException;
import com.example.arbomachine.domain.user.exception.UserAccountNotFoundException;
import com.example.arbomachine.domain.user.mapper.UserAccountMapper;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Operacje admina na cudzych kontach. Dostep tylko dla roli ADMIN (pilnuje SecurityConfig dla /api/admin/**).
 * <p>
 * Konta administratorow zarzadzane sa wylacznie w bazie, dlatego kazda operacja zmieniajaca
 * odmawia dzialania na koncie z rola ADMIN.
 */
@Service
public class AdminUserAccountManagementService {
    private final UserAccountRepository userAccountRepository;
    private final UserAccountMapper userAccountMapper;
    private final UserProfileUpdateService userProfileUpdateService;
    private final UserAccountDeletionService userAccountDeletionService;

    public AdminUserAccountManagementService(UserAccountRepository userAccountRepository,
                                             UserAccountMapper userAccountMapper,
                                             UserProfileUpdateService userProfileUpdateService,
                                             UserAccountDeletionService userAccountDeletionService) {
        this.userAccountRepository = userAccountRepository;
        this.userAccountMapper = userAccountMapper;
        this.userProfileUpdateService = userProfileUpdateService;
        this.userAccountDeletionService = userAccountDeletionService;
    }

    /** Lista kont posortowana po id; roleFilter == null oznacza wszystkie role. */
    public List<UserAccountDto> listUserAccounts(UserRole roleFilter) {
        List<UserAccount> userAccounts = roleFilter == null
                ? userAccountRepository.findAll()
                : userAccountRepository.findByRole(roleFilter);
        return userAccounts.stream()
                .sorted(Comparator.comparing(UserAccount::getId))
                .map(userAccountMapper::toUserAccountDto)
                .toList();
    }

    public UserAccountDto getUserAccountById(Long userAccountId) {
        return userAccountMapper.toUserAccountDto(findUserAccountById(userAccountId));
    }

    public UserAccountStatisticsDto getUserAccountStatistics() {
        return new UserAccountStatisticsDto(
                userAccountRepository.count(),
                userAccountRepository.countByRole(UserRole.ARBORIST),
                userAccountRepository.countByRole(UserRole.OFFICIAL),
                userAccountRepository.countByRole(UserRole.ADMIN),
                userAccountRepository.countByEmailConfirmed(false),
                userAccountRepository.countByLockedByAdmin(true)
        );
    }

    public UserAccountDto updateUserProfileAsAdmin(Long userAccountId, UserProfileDto updatedProfile) {
        findUserAccountManageableByAdmin(userAccountId);
        userProfileUpdateService.updateUserProfile(userAccountId, updatedProfile);
        return getUserAccountById(userAccountId);
    }

    public UserAccountDto changeUserRole(Long userAccountId, AssignableUserRole newRole) {
        UserAccount userAccount = findUserAccountManageableByAdmin(userAccountId);
        userAccount.setRole(newRole.toUserRole());
        return userAccountMapper.toUserAccountDto(userAccountRepository.save(userAccount));
    }

    /** Zablokowane konto traci dostep od razu - filtr JWT odrzuca takze wczesniej wydane tokeny. */
    public UserAccountDto setAccountLockedByAdmin(Long userAccountId, boolean lockedByAdmin) {
        UserAccount userAccount = findUserAccountManageableByAdmin(userAccountId);
        userAccount.setLockedByAdmin(lockedByAdmin);
        return userAccountMapper.toUserAccountDto(userAccountRepository.save(userAccount));
    }

    /** Potwierdza e-mail za uzytkownika, np. gdy mail aktywacyjny nie dotarl. */
    public UserAccountDto confirmEmailOnUsersBehalf(Long userAccountId) {
        UserAccount userAccount = findUserAccountManageableByAdmin(userAccountId);
        userAccount.setEmailConfirmed(true);
        return userAccountMapper.toUserAccountDto(userAccountRepository.save(userAccount));
    }

    public void deleteUserAccountAsAdmin(Long userAccountId) {
        findUserAccountManageableByAdmin(userAccountId);
        userAccountDeletionService.deleteUserAccountById(userAccountId);
    }

    private UserAccount findUserAccountById(Long userAccountId) {
        return userAccountRepository.findById(userAccountId)
                .orElseThrow(() -> new UserAccountNotFoundException("User not found"));
    }

    private UserAccount findUserAccountManageableByAdmin(Long userAccountId) {
        UserAccount userAccount = findUserAccountById(userAccountId);
        if (userAccount.getRole() == UserRole.ADMIN) {
            throw new AdminAccountModificationForbiddenException(
                    "Admin accounts can only be managed directly in the database");
        }
        return userAccount;
    }
}

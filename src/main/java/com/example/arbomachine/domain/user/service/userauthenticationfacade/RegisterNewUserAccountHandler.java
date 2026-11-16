package com.example.arbomachine.domain.user.service.userauthenticationfacade;

import com.example.arbomachine.domain.user.dto.UserProfileDto;
import com.example.arbomachine.domain.user.entity.EmailConfirmationToken;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.AssignableUserRole;
import com.example.arbomachine.domain.demo.DemoAccountPolicy;
import com.example.arbomachine.domain.user.exception.AccountTypeRegistrationClosedException;
import com.example.arbomachine.domain.user.exception.EmailAlreadyInUseException;
import com.example.arbomachine.domain.user.mapper.UserAccountMapper;
import com.example.arbomachine.infrastructure.repository.EmailConfirmationTokenRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import com.example.arbomachine.infrastructure.service.EmailSenderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

/** Zakladanie nowego konta (arborysta / urzednik) i wysylka maila z linkiem aktywacyjnym. */
@Component
public class RegisterNewUserAccountHandler {
    private static final String CONFIRMATION_EMAIL_SUBJECT = "Potwierdź rejestrację w ArboMachine";
    private static final int CONFIRMATION_LINK_VALIDITY_DAYS = 1;

    private final UserAccountRepository userAccountRepository;
    private final EmailConfirmationTokenRepository emailConfirmationTokenRepository;
    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailSenderService emailSenderService;

    // Inicjalizatory pol dzialaja jako wartosci domyslne w testach jednostkowych (tam Spring nie wstrzykuje @Value).
    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl = "http://localhost:5173";

    @Value("${app.registration.email-confirmation-required:true}")
    private boolean emailConfirmationRequired = true;

    /** Rejestracja kont urzednika - chwilowo zamknieta (istniejace konta urzednikow dzialaja dalej). */
    @Value("${app.registration.official-enabled:false}")
    private boolean officialRegistrationEnabled = false;

    /** Adres konta trybu demo - nie da sie go zajac rejestracja (konto zaklada seeder demo). */
    @Value("${app.demo.account-email:" + DemoAccountPolicy.DEFAULT_DEMO_ACCOUNT_EMAIL + "}")
    private String demoAccountEmail = DemoAccountPolicy.DEFAULT_DEMO_ACCOUNT_EMAIL;

    public RegisterNewUserAccountHandler(UserAccountRepository userAccountRepository,
                                         EmailConfirmationTokenRepository emailConfirmationTokenRepository,
                                         UserAccountMapper userAccountMapper,
                                         PasswordEncoder passwordEncoder,
                                         EmailSenderService emailSenderService) {
        this.userAccountRepository = userAccountRepository;
        this.emailConfirmationTokenRepository = emailConfirmationTokenRepository;
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.emailSenderService = emailSenderService;
    }

    /**
     * Zaklada konto z wybrana rola i wysyla link aktywacyjny.
     * <p>
     * Ponowna rejestracja na adres, ktory nie zostal jeszcze potwierdzony, nadpisuje to samo konto
     * i wysyla nowy link (zamiast tworzyc duplikat z tym samym e-mailem). Kont potwierdzonych
     * i zablokowanych przez admina nie da sie w ten sposob przejac.
     */
    public void handle(UserProfileDto userProfile, String plainTextPassword, AssignableUserRole requestedRole) {
        if (requestedRole == AssignableUserRole.OFFICIAL && !officialRegistrationEnabled) {
            throw new AccountTypeRegistrationClosedException("Registration of this account type is currently closed");
        }
        if (userProfile.email() != null
                && userProfile.email().trim().toLowerCase(Locale.ROOT).equals(demoAccountEmail.trim().toLowerCase(Locale.ROOT))) {
            throw new EmailAlreadyInUseException("Email is already in use by an active user");
        }
        UserAccount existingAccountWithSameEmail = userAccountRepository.findByEmail(userProfile.email()).orElse(null);
        if (existingAccountWithSameEmail != null
                && (existingAccountWithSameEmail.isEmailConfirmed() || existingAccountWithSameEmail.isLockedByAdmin())) {
            throw new EmailAlreadyInUseException("Email is already in use by an active user");
        }

        UserAccount userAccount;
        if (existingAccountWithSameEmail != null) {
            userAccount = existingAccountWithSameEmail;
            copyProfileDataIntoAccount(userProfile, userAccount);
        } else {
            userAccount = userAccountMapper.toNewUserAccountEntity(userProfile);
        }
        userAccount.setPasswordHash(passwordEncoder.encode(plainTextPassword));
        userAccount.setRole(requestedRole.toUserRole());
        userAccount.setEmailConfirmed(!emailConfirmationRequired);
        userAccountRepository.save(userAccount);

        if (!emailConfirmationRequired) {
            return;
        }

        String confirmationToken = createAndSaveEmailConfirmationToken(userAccount);
        String confirmationLink = frontendBaseUrl + "/confirm?token=" + confirmationToken;
        emailSenderService.sendRegistrationConfirmationEmail(userAccount.getEmail(), CONFIRMATION_EMAIL_SUBJECT,
                userAccount.getFirstName(), userAccount.getLastName(), confirmationLink);
    }

    private void copyProfileDataIntoAccount(UserProfileDto userProfile, UserAccount userAccount) {
        userAccount.setFirstName(userProfile.firstName());
        userAccount.setLastName(userProfile.lastName());
        userAccount.setPhoneNumber(userProfile.phoneNumber());
        userAccount.setStreet(userProfile.street());
        userAccount.setPostalCode(userProfile.postalCode());
        userAccount.setCity(userProfile.city());
    }

    private String createAndSaveEmailConfirmationToken(UserAccount userAccount) {
        String token = UUID.randomUUID().toString();
        EmailConfirmationToken emailConfirmationToken = EmailConfirmationToken.builder()
                .token(token)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(CONFIRMATION_LINK_VALIDITY_DAYS))
                .userAccount(userAccount)
                .build();
        emailConfirmationTokenRepository.save(emailConfirmationToken);
        return token;
    }
}

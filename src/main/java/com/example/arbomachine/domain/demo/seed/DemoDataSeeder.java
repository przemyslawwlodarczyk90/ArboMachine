package com.example.arbomachine.domain.demo.seed;

import com.example.arbomachine.domain.demo.DemoAccountPolicy;
import com.example.arbomachine.domain.documentauthor.entity.DocumentAuthorProfile;
import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.treemonitoring.entity.TreeMonitoringRecord;
import com.example.arbomachine.domain.treework.entity.TreeWork;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.enums.UserRole;
import com.example.arbomachine.infrastructure.repository.DocumentAuthorProfileRepository;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import com.example.arbomachine.infrastructure.repository.TreeMonitoringRecordRepository;
import com.example.arbomachine.infrastructure.repository.TreeWorkRepository;
import com.example.arbomachine.infrastructure.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Przygotowanie trybu demo przy starcie aplikacji (tylko przy app.demo.enabled=true):
 * <ol>
 *   <li>konto demo (rola ARBORIST, e-mail potwierdzony, losowe nieznane haslo - logowanie haslem jest niemozliwe),</li>
 *   <li>projekty pokazowe - tylko gdy konto demo nie ma jeszcze zadnego projektu (ponowny start nie dubluje danych),</li>
 *   <li>przesuniecie dat (zlecenia, prac, kontroli) tak, zeby terminy zawsze byly liczone od dzisiaj - przy starcie
 *       i codziennie po polnocy; inaczej po kilku tygodniach filtr "najblizsze 7 dni" bylby pusty.</li>
 * </ol>
 * Blad seedowania jest logowany i nie zatrzymuje aplikacji - demo bedzie wtedy niedostepne (GET /api/demo = false).
 */
@Component
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    private final DemoAccountPolicy demoAccountPolicy;
    private final UserAccountRepository userAccountRepository;
    private final ProjectRepository projectRepository;
    private final TreeWorkRepository treeWorkRepository;
    private final TreeMonitoringRecordRepository treeMonitoringRecordRepository;
    private final DocumentAuthorProfileRepository documentAuthorProfileRepository;
    private final DemoProjectsWriter demoProjectsWriter;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;

    public DemoDataSeeder(DemoAccountPolicy demoAccountPolicy, UserAccountRepository userAccountRepository,
                          ProjectRepository projectRepository, TreeWorkRepository treeWorkRepository,
                          TreeMonitoringRecordRepository treeMonitoringRecordRepository,
                          DocumentAuthorProfileRepository documentAuthorProfileRepository,
                          DemoProjectsWriter demoProjectsWriter, PasswordEncoder passwordEncoder,
                          PlatformTransactionManager transactionManager) {
        this.demoAccountPolicy = demoAccountPolicy;
        this.userAccountRepository = userAccountRepository;
        this.projectRepository = projectRepository;
        this.treeWorkRepository = treeWorkRepository;
        this.treeMonitoringRecordRepository = treeMonitoringRecordRepository;
        this.documentAuthorProfileRepository = documentAuthorProfileRepository;
        this.demoProjectsWriter = demoProjectsWriter;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments arguments) {
        try {
            prepareDemoData();
        } catch (RuntimeException seedingError) {
            LOGGER.error("Demo data could not be prepared - demo mode will be unavailable", seedingError);
        }
    }

    /** Codziennie po polnocy: terminy prac i kontroli pozostaja aktualne wzgledem dzisiejszej daty. */
    @Scheduled(cron = "${app.demo.date-refresh-cron:0 5 0 * * *}", zone = "Europe/Warsaw")
    public void refreshDemoDatesDaily() {
        try {
            transactionTemplate.executeWithoutResult(status -> shiftDatesToToday(LocalDate.now(WARSAW)));
        } catch (RuntimeException refreshError) {
            LOGGER.warn("Demo dates could not be refreshed", refreshError);
        }
    }

    void prepareDemoData() {
        LocalDate today = LocalDate.now(WARSAW);
        transactionTemplate.executeWithoutResult(status -> {
            UserAccount demoAccount = ensureDemoAccount();
            if (projectRepository.existsByOwnerId(demoAccount.getId())) {
                shiftDatesToToday(today);
                LOGGER.info("Demo data already present for {}", demoAccount.getEmail());
                return;
            }
            ensureDocumentAuthorProfile(demoAccount);
            int treeCount = demoProjectsWriter.writeAllProjects(demoAccount, today);
            demoProjectsWriter.writeNotifications(demoAccount);
            LOGGER.info("Demo data created: {} trees in demo projects", treeCount);
        });
    }

    private UserAccount ensureDemoAccount() {
        String email = demoAccountPolicy.demoAccountEmail();
        UserAccount account = userAccountRepository.findByEmail(email).orElseGet(UserAccount::new);
        account.setEmail(email);
        account.setFirstName("Konto");
        account.setLastName("demonstracyjne");
        account.setCity("Zielonowo");
        account.setRole(UserRole.ARBORIST);
        account.setEmailConfirmed(true);
        account.setLockedByAdmin(false);
        // nowe losowe haslo przy kazdym starcie - nikt go nie zna, wejscie tylko przez POST /api/demo/session
        account.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        return userAccountRepository.save(account);
    }

    private void ensureDocumentAuthorProfile(UserAccount demoAccount) {
        if (documentAuthorProfileRepository.findByUserAccountId(demoAccount.getId()).isPresent()) return;
        documentAuthorProfileRepository.save(DocumentAuthorProfile.createFor(demoAccount, new DocumentAuthorProfile.Details(
                "Jan Przykładowy", "Arborysta (dane przykładowe trybu demo)", "Pracownia Arborystyczna „Przykład”",
                "ul. Przykładowa 1, 00-000 Zielonowo",
                "Obwód pnia mierzony taśmą na wysokości 130 cm, wysokość dalmierzem, lokalizacja z GPS tabletu (średnia z serii). "
                        + "Ocena stanu wzrokowa z poziomu gruntu w skali aplikacji: dobry / umiarkowany / zły."),
                Instant.now().truncatedTo(ChronoUnit.MICROS)));
    }

    /** Przesuwa wszystkie daty projektow demo o tyle dni, ile minelo od ostatniego wyrownania (kotwica: park). */
    private void shiftDatesToToday(LocalDate today) {
        UserAccount demoAccount = userAccountRepository.findByEmail(demoAccountPolicy.demoAccountEmail()).orElse(null);
        if (demoAccount == null) return;
        List<Project> projects = projectRepository.findAllByOwnerIdOrderByCreatedAtDesc(demoAccount.getId());
        Project anchor = projects.stream()
                .filter(project -> DemoProjectsWriter.ANCHOR_PROJECT_NAME.equals(project.getName()) && project.getOrderDate() != null)
                .findFirst().orElse(null);
        if (anchor == null) return;
        long shiftDays = ChronoUnit.DAYS.between(anchor.getOrderDate().plusDays(DemoProjectsWriter.ANCHOR_ORDER_DAYS_AGO), today);
        if (shiftDays <= 0) return;
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        for (Project project : projects) {
            if (project.getOrderDate() != null) project.setOrderDate(project.getOrderDate().plusDays(shiftDays));
            for (TreeWork work : treeWorkRepository.findAllByTreeProjectIdOrderByTreeIdAscCreatedAtAscIdAsc(project.getId())) {
                TreeWork.Fields fields = work.fields();
                work.replaceFields(new TreeWork.Fields(fields.name(), fields.description(), fields.status(), fields.priority(),
                        plus(fields.plannedDate(), shiftDays), plus(fields.completedDate(), shiftDays)), now);
            }
            for (TreeMonitoringRecord record : treeMonitoringRecordRepository.findAllOfProjectNewestFirst(project.getId())) {
                TreeMonitoringRecord.Fields fields = record.fields();
                record.replaceFields(new TreeMonitoringRecord.Fields(plus(fields.inspectionDate(), shiftDays), fields.healthStatus(),
                        fields.notes(), plus(fields.nextInspectionDate(), shiftDays)), now);
            }
        }
        LOGGER.info("Demo dates shifted by {} days", shiftDays);
    }

    private static LocalDate plus(LocalDate date, long days) {
        return date == null ? null : date.plusDays(days);
    }
}

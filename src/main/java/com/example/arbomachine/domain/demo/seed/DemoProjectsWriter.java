package com.example.arbomachine.domain.demo.seed;

import com.example.arbomachine.domain.demo.seed.DemoSampleData.Species;
import com.example.arbomachine.domain.demo.seed.DemoTreeIllustrator.AnnotationStroke;
import com.example.arbomachine.domain.demo.seed.DemoTreeIllustrator.Shot;
import com.example.arbomachine.domain.notification.entity.UserNotification;
import com.example.arbomachine.domain.notification.entity.UserNotificationType;
import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.entity.TreeEditableAttributes;
import com.example.arbomachine.domain.tree.entity.TreeLocation;
import com.example.arbomachine.domain.tree.entity.TreeLocationSource;
import com.example.arbomachine.domain.treemarking.entity.TreeMarking;
import com.example.arbomachine.domain.treemonitoring.entity.TreeHealthStatus;
import com.example.arbomachine.domain.treemonitoring.entity.TreeMonitoringRecord;
import com.example.arbomachine.domain.treephoto.annotation.PhotoAnnotationLimits;
import com.example.arbomachine.domain.treephoto.dto.NormalizedPointDto;
import com.example.arbomachine.domain.treephoto.dto.PhotoAnnotationDocumentRequest;
import com.example.arbomachine.domain.treephoto.dto.PhotoAnnotationElementDto;
import com.example.arbomachine.domain.treephoto.entity.TreePhoto;
import com.example.arbomachine.domain.treephoto.entity.TreePhotoAnnotation;
import com.example.arbomachine.domain.treephoto.storage.PhotoStorage;
import com.example.arbomachine.domain.treestand.entity.TreeStand;
import com.example.arbomachine.domain.treestand.entity.TreeStandSource;
import com.example.arbomachine.domain.treestand.geometry.StandPolygon;
import com.example.arbomachine.domain.treevaluation.entity.TreeValuation;
import com.example.arbomachine.domain.treevaluation.entity.TreeValuationCategory;
import com.example.arbomachine.domain.treework.entity.TreeWork;
import com.example.arbomachine.domain.treework.entity.TreeWorkPriority;
import com.example.arbomachine.domain.treework.entity.TreeWorkStatus;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import com.example.arbomachine.infrastructure.repository.TreeMarkingRepository;
import com.example.arbomachine.infrastructure.repository.TreeMonitoringRecordRepository;
import com.example.arbomachine.infrastructure.repository.TreePhotoAnnotationRepository;
import com.example.arbomachine.infrastructure.repository.TreePhotoRepository;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import com.example.arbomachine.infrastructure.repository.TreeStandRepository;
import com.example.arbomachine.infrastructure.repository.TreeValuationRepository;
import com.example.arbomachine.infrastructure.repository.TreeWorkRepository;
import com.example.arbomachine.infrastructure.repository.UserNotificationRepository;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static com.example.arbomachine.domain.demo.seed.DemoSampleData.*;

/**
 * Zapis czterech projektow pokazowych trybu demo bezposrednio przez encje i repozytoria (konto demo nie moze pisac
 * przez API). Dane sa deterministyczne (stale ziarno), spojne miedzy lista, mapa, kartami drzew i dokumentem PDF,
 * a daty sa liczone wzgledem dnia seedowania (DemoDataSeeder przesuwa je potem codziennie).
 */
@Component
public class DemoProjectsWriter {

    /** Projekt-kotwica dat: jego data zlecenia to zawsze "dzis minus ANCHOR_ORDER_DAYS_AGO" (patrz DemoDataSeeder). */
    static final String ANCHOR_PROJECT_NAME = "Park „Zielona Polana” – inwentaryzacja drzewostanu";
    static final int ANCHOR_ORDER_DAYS_AGO = 60;

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final String DEMO_DESCRIPTION_SUFFIX = " Projekt pokazowy trybu demo – wszystkie dane są fikcyjne.";

    private final ProjectRepository projectRepository;
    private final TreeRepository treeRepository;
    private final TreeValuationRepository treeValuationRepository;
    private final TreeMonitoringRecordRepository treeMonitoringRecordRepository;
    private final TreeWorkRepository treeWorkRepository;
    private final TreeStandRepository treeStandRepository;
    private final TreeMarkingRepository treeMarkingRepository;
    private final TreePhotoRepository treePhotoRepository;
    private final TreePhotoAnnotationRepository treePhotoAnnotationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final PhotoStorage photoStorage;
    private final ObjectMapper objectMapper;

    private Instant now;
    private LocalDate today;
    private Random random;

    public DemoProjectsWriter(ProjectRepository projectRepository, TreeRepository treeRepository,
                              TreeValuationRepository treeValuationRepository,
                              TreeMonitoringRecordRepository treeMonitoringRecordRepository,
                              TreeWorkRepository treeWorkRepository, TreeStandRepository treeStandRepository,
                              TreeMarkingRepository treeMarkingRepository, TreePhotoRepository treePhotoRepository,
                              TreePhotoAnnotationRepository treePhotoAnnotationRepository,
                              UserNotificationRepository userNotificationRepository, PhotoStorage photoStorage,
                              ObjectMapper objectMapper) {
        this.projectRepository = projectRepository;
        this.treeRepository = treeRepository;
        this.treeValuationRepository = treeValuationRepository;
        this.treeMonitoringRecordRepository = treeMonitoringRecordRepository;
        this.treeWorkRepository = treeWorkRepository;
        this.treeStandRepository = treeStandRepository;
        this.treeMarkingRepository = treeMarkingRepository;
        this.treePhotoRepository = treePhotoRepository;
        this.treePhotoAnnotationRepository = treePhotoAnnotationRepository;
        this.userNotificationRepository = userNotificationRepository;
        this.photoStorage = photoStorage;
        this.objectMapper = objectMapper;
    }

    /** Wywolywane w jednej transakcji przez DemoDataSeeder. Zwraca liczbe utworzonych drzew. */
    synchronized int writeAllProjects(UserAccount owner, LocalDate seedingDay) {
        this.now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        this.today = seedingDay;
        this.random = new Random(20_261_116L);
        this.markedTrees.clear();
        int treeCount = 0;
        // Kolejnosc zapisu = odwrotna kolejnosc na liscie projektow (lista: najnowsze na gorze) - park ma byc pierwszy.
        treeCount += writeSchoolProject(owner);
        treeCount += writeCemeteryProject(owner);
        treeCount += writeAlleyProject(owner);
        treeCount += writeParkProject(owner);
        return treeCount;
    }

    // ---------------------------------------------------------------- projekt 1: park (duza inwentaryzacja)

    private int writeParkProject(UserAccount owner) {
        Project project = newProject(owner, ANCHOR_PROJECT_NAME,
                "Pełna inwentaryzacja drzewostanu parku miejskiego: pomiary, lokalizacja GPS, waloryzacja, grupy drzew "
                        + "i zalecenia pielęgnacyjne.",
                "Zarząd Zieleni Miejskiej w Zielonowie", "Park „Zielona Polana”, ul. Parkowa 1, 00-000 Zielonowo",
                ANCHOR_ORDER_DAYS_AGO, "ZZM/INW/2026/014",
                "Inwentaryzacja dendrologiczna drzewostanu parku na potrzeby planu rewaloryzacji.",
                "Umowa z Zamawiającym. Ustawa z dnia 16 kwietnia 2004 r. o ochronie przyrody.");
        double centerLatitude = 51.76010, centerLongitude = 19.40280, halfLatitude = 0.0023, halfLongitude = 0.0037;

        List<List<Species>> speciesByQuarter = List.of(
                List.of(DAB_SZYPULKOWY, DAB_SZYPULKOWY, LIPA_DROBNOLISTNA, BUK, KLON_JAWOR, JESION, WIERZBA, OLSZA),
                List.of(LIPA_DROBNOLISTNA, KLON_ZWYCZAJNY, KASZTANOWIEC, PLATAN, GRAB, DAB_SZYPULKOWY, MODRZEW),
                List.of(KLON_ZWYCZAJNY, GRAB, JARZAB, ROBINIA, SOSNA, SWIERK, LIPA_DROBNOLISTNA),
                List.of(BRZOZA, BRZOZA, BRZOZA, SOSNA, SOSNA, DAGLEZJA, TOPOLA, BRZOZA));

        List<Tree> trees = new ArrayList<>();
        for (int index = 1; index <= 110; index++) {
            double radius = Math.sqrt(random.nextDouble()) * 0.94;
            double angle = random.nextDouble() * 2 * Math.PI;
            double latitude = centerLatitude + Math.sin(angle) * radius * halfLatitude;
            double longitude = centerLongitude + Math.cos(angle) * radius * halfLongitude;
            int quarter = (latitude >= centerLatitude ? 0 : 2) + (longitude >= centerLongitude ? 1 : 0);
            Species species = pick(random, speciesByQuarter.get(quarter));
            Instant measuredAt = measurementTime(ANCHOR_ORDER_DAYS_AGO - 5 - index / 30);
            boolean byMap = index % 9 == 0;
            Tree tree = newTree(project, String.valueOf(index), species, randomCircumference(species), latitude, longitude,
                    byMap ? TreeLocationSource.MAP : TreeLocationSource.DEVICE_GNSS,
                    byMap ? null : decimal(2.4 + random.nextDouble() * 5.5), byMap ? null : measuredAt,
                    byMap ? null : "browser-geolocation-averaged");
            trees.add(tree);
        }

        for (Tree tree : trees) {
            valuate(tree, categoryFromCircumference(tree));
            int circumference = tree.getTrunkCircumferenceCm();
            if (circumference > 330) mark(tree, "VALUABLE");
            if (circumference > 420) mark(tree, "MONUMENT_CANDIDATE");
            if (random.nextInt(12) == 0) mark(tree, "HOLLOW");
            if (random.nextInt(14) == 0) mark(tree, "NEST");
            if (random.nextInt(10) < 7) {
                TreeHealthStatus health = randomHealth(70, 24);
                inspect(tree, ANCHOR_ORDER_DAYS_AGO - 8, health, 365);
                planWorksForHealth(tree, health, 10);
            }
        }
        for (int index = 0; index < 16; index++) {
            Tree tree = trees.get(index * 6 + 2);
            addPhoto(tree, Shot.WHOLE_TREE, index % 5 == 0, "Widok ogólny drzewa nr " + tree.getInventoryNumber() + ".", true, false);
        }

        double[] corners = {centerLongitude - halfLongitude, centerLongitude, centerLongitude + halfLongitude,
                centerLatitude - halfLatitude, centerLatitude, centerLatitude + halfLatitude};
        createStand(project, trees, "Starodrzew przy stawie", "Najstarsze dęby i buki parku; kilka drzew o cechach pomnikowych.",
                rectangle(corners[0], corners[4], corners[1], corners[5]));
        createStand(project, trees, "Polana centralna", "Drzewa soliterowe i szpaler lip wzdłuż alei głównej.",
                rectangle(corners[1], corners[4], corners[2], corners[5]));
        createStand(project, trees, "Zadrzewienie przy placu zabaw", "Strefa intensywnego użytkowania – priorytet bezpieczeństwa.",
                rectangle(corners[0], corners[3], corners[1], corners[4]));
        createStand(project, trees, "Brzezina południowa", "Młodszy drzewostan brzozowo-sosnowy, naturalne odnowienie.",
                rectangle(corners[1], corners[3], corners[2], corners[4]));
        return trees.size();
    }

    // ---------------------------------------------------------------- projekt 2: aleja (ocena stanu, historia kontroli)

    private int writeAlleyProject(UserAccount owner) {
        Project project = newProject(owner, "Aleja lipowa przy ul. Klonowej – ocena stanu drzew",
                "Ocena stanu zdrowotnego i bezpieczeństwa drzew w alei przydrożnej przed przebudową chodnika. "
                        + "Historia kontroli z trzech sezonów.",
                "Gmina Zielonowo – Wydział Infrastruktury", "ul. Klonowa (od ul. Polnej do ul. Leśnej), 00-000 Zielonowo",
                35, "WI.7021.3.2026",
                "Ocena stanu drzew i zalecenia przed przebudową chodnika w ciągu ul. Klonowej.",
                "Umowa z Zamawiającym. Ustawa z dnia 16 kwietnia 2004 r. o ochronie przyrody.");
        double startLatitude = 51.75700, startLongitude = 19.39900, endLatitude = 51.75760, endLongitude = 19.41100;
        List<Tree> trees = new ArrayList<>();
        for (int index = 0; index < 40; index++) {
            int row = index % 2;
            double position = (index / 2) / 19.0;
            double latitude = startLatitude + (endLatitude - startLatitude) * position + (row == 0 ? 0.00009 : -0.00009)
                    + (random.nextDouble() - 0.5) * 0.00002;
            double longitude = startLongitude + (endLongitude - startLongitude) * position + (random.nextDouble() - 0.5) * 0.00006;
            boolean replacement = index == 7 || index == 18 || index == 29 || index == 33 || index == 36;
            Species species = replacement ? KLON_ZWYCZAJNY : index % 8 == 5 ? LIPA_SZEROKOLISTNA : LIPA_DROBNOLISTNA;
            int circumference = replacement ? 30 + random.nextInt(25) : 160 + random.nextInt(170);
            Tree tree = newTree(project, "A-%02d".formatted(index + 1), species, circumference, latitude, longitude,
                    TreeLocationSource.IMPORT, null, null, null);
            trees.add(tree);
            if (replacement) {
                // nasadzenia zastepcze sprzed roku - jeszcze bez kontroli ("Nie sprawdzone")
                valuate(tree, TreeValuationCategory.LOW);
                continue;
            }
            TreeHealthStatus current = randomHealth(48, 34);
            TreeHealthStatus lastYear = current == TreeHealthStatus.POOR ? TreeHealthStatus.FAIR : current;
            TreeHealthStatus twoYearsAgo = lastYear == TreeHealthStatus.FAIR && random.nextBoolean() ? TreeHealthStatus.GOOD : lastYear;
            inspect(tree, 35 + 730, twoYearsAgo, 0);
            inspect(tree, 35 + 365, lastYear, 0);
            inspect(tree, 20, current, current == TreeHealthStatus.POOR ? 90 : 365);
            valuate(tree, circumference > 280 ? TreeValuationCategory.HIGH : TreeValuationCategory.MEDIUM);
            if (index % 3 == 0) mark(tree, "INFRASTRUCTURE_CONFLICT");
            planWorksForHealth(tree, current, 3);
            if (current == TreeHealthStatus.POOR) {
                addPhoto(tree, Shot.TRUNK_DETAIL, false,
                        "Drzewo " + tree.getInventoryNumber() + " – ubytek u podstawy pnia i owocnik grzyba (zaznaczono).", true, true);
                if (index % 2 == 0) {
                    addPhoto(tree, Shot.CROWN_DETAIL, false,
                            "Drzewo " + tree.getInventoryNumber() + " – posusz w koronie nad chodnikiem.", true, true);
                }
            }
            if (index % 3 == 0) {
                addWork(tree, "Kontrola systemu korzeniowego podczas przebudowy chodnika",
                        "Nadzór arborystyczny przy wykopach w strefie korzeniowej.", TreeWorkStatus.PLANNED,
                        TreeWorkPriority.NORMAL, 21 + random.nextInt(20), null, "BLUE_SQUARE");
            }
        }
        createStand(project, trees, "Aleja lipowa – ul. Klonowa", "Dwurzędowa aleja lipowa, ok. 70 lat.",
                List.of(new double[]{startLongitude - 0.0002, startLatitude - 0.0002}, new double[]{endLongitude + 0.0002, endLatitude - 0.0002},
                        new double[]{endLongitude + 0.0002, endLatitude + 0.0002}, new double[]{startLongitude - 0.0002, startLatitude + 0.0002}));
        return trees.size();
    }

    // ---------------------------------------------------------------- projekt 3: cmentarz (dokumentacja fotograficzna)

    private int writeCemeteryProject(UserAccount owner) {
        Project project = newProject(owner, "Cmentarz parafialny w Zielonowie – dokumentacja fotograficzna",
                "Dokumentacja fotograficzna i opis stanu starodrzewu. Zdjęcia z adnotacjami wskazują ubytki i posusz.",
                "Zarządca cmentarza parafialnego w Zielonowie", "ul. Cmentarna 5, 00-000 Zielonowo",
                25, "CP/2026/07",
                "Dokumentacja fotograficzna i ocena stanu starodrzewu na terenie zabytkowego cmentarza.",
                "Umowa z Zamawiającym. Ustawa z dnia 16 kwietnia 2004 r. o ochronie przyrody.");
        List<Species> oldTrees = List.of(DAB_SZYPULKOWY, LIPA_DROBNOLISTNA, JESION, KASZTANOWIEC, CIS, KLON_JAWOR, ZYWOTNIK,
                LIPA_DROBNOLISTNA, DAB_SZYPULKOWY, SWIERK);
        double centerLatitude = 51.75560, centerLongitude = 19.41620;
        List<Tree> trees = new ArrayList<>();
        for (int index = 0; index < 24; index++) {
            Species species = oldTrees.get(index % oldTrees.size());
            double latitude = centerLatitude + ((index / 6) - 1.5) * 0.00042 + (random.nextDouble() - 0.5) * 0.00012;
            double longitude = centerLongitude + ((index % 6) - 2.5) * 0.00040 + (random.nextDouble() - 0.5) * 0.00012;
            int circumference = Math.max(species.minimumCircumferenceCm(),
                    species.maximumCircumferenceCm() - random.nextInt(Math.max(1, species.maximumCircumferenceCm() / 3)));
            Tree tree = newTree(project, "C-%d".formatted(index + 1), species, circumference, latitude, longitude,
                    TreeLocationSource.MAP, null, null, null);
            trees.add(tree);
            valuate(tree, circumference > 380 ? TreeValuationCategory.VERY_HIGH : TreeValuationCategory.HIGH);
            TreeHealthStatus health = randomHealth(50, 35);
            inspect(tree, 18, health, health == TreeHealthStatus.POOR ? 180 : 365);
            planWorksForHealth(tree, health, 14);

            boolean autumn = !species.conifer() && index % 3 == 0;
            addPhoto(tree, Shot.WHOLE_TREE, autumn,
                    "Drzewo " + tree.getInventoryNumber() + " (" + species.polish() + ") – widok ogólny od strony alejki.", true, index % 4 == 1);
            boolean annotatedTrunk = index % 2 == 0;
            addPhoto(tree, Shot.TRUNK_DETAIL, false, annotatedTrunk
                            ? "Pień – ubytek wgłębny i owocnik grzyba u nasady (zaznaczono na zdjęciu)."
                            : "Pień – stan kory i nasady.", annotatedTrunk, annotatedTrunk);
            if (index % 3 != 2) {
                boolean annotatedCrown = index % 6 == 0;
                addPhoto(tree, Shot.CROWN_DETAIL, autumn, annotatedCrown
                        ? "Korona – suchy konar nad alejką (zaznaczono)." : "Korona – fragment z przewodnikami głównymi.",
                        annotatedCrown, annotatedCrown);
            }
        }
        mark(trees.get(0), "NATURE_MONUMENT");
        mark(trees.get(8), "MONUMENT_CANDIDATE");
        mark(trees.get(10), "MONUMENT_CANDIDATE");
        mark(trees.get(18), "MONUMENT_CANDIDATE");
        mark(trees.get(0), "PROTECTED_SPECIES");
        mark(trees.get(2), "HOLLOW");
        mark(trees.get(4), "TO_PROTECT");
        mark(trees.get(12), "FUNGI");
        createStand(project, trees, "Kwatera zabytkowa", "Najstarsza część cmentarza z alejami dębowo-lipowymi.",
                rectangle(centerLongitude - 0.00125, centerLatitude - 0.00095, centerLongitude, centerLatitude + 0.00095));
        return trees.size();
    }

    // ---------------------------------------------------------------- projekt 4: szkola (plan prac)

    private int writeSchoolProject(UserAccount owner) {
        Project project = newProject(owner, "Teren Szkoły Podstawowej nr 3 – plan prac pielęgnacyjnych",
                "Plan prac pielęgnacyjnych i zabezpieczających drzewa przy boisku i wejściu do szkoły – terminy, priorytety, "
                        + "symbole prac na mapie.",
                "Szkoła Podstawowa nr 3 w Zielonowie", "ul. Szkolna 12, 00-000 Zielonowo",
                15, "SP3/ZP/2026/02",
                "Plan prac pielęgnacyjnych drzew na terenie szkoły z uwzględnieniem bezpieczeństwa uczniów.",
                "Umowa z Zamawiającym.");
        List<Species> mix = List.of(BRZOZA, KLON_ZWYCZAJNY, SOSNA, SWIERK, ROBINIA, WIERZBA, TOPOLA, LIPA_DROBNOLISTNA, JARZAB, KASZTANOWIEC);
        double centerLatitude = 51.76260, centerLongitude = 19.41450;
        List<Tree> trees = new ArrayList<>();
        for (int index = 0; index < 30; index++) {
            Species species = pick(random, mix);
            double latitude = centerLatitude + (random.nextDouble() - 0.5) * 0.0010;
            double longitude = centerLongitude + (random.nextDouble() - 0.5) * 0.0016;
            Tree tree = newTree(project, "S-%02d".formatted(index + 1), species, randomCircumference(species), latitude, longitude,
                    TreeLocationSource.DEVICE_GNSS, decimal(3 + random.nextDouble() * 6), measurementTime(12), "browser-geolocation");
            trees.add(tree);
            if (index % 2 == 0) valuate(tree, categoryFromCircumference(tree));
            TreeHealthStatus health = randomHealth(60, 30);
            inspect(tree, 12, health, 180);
            if (index % 5 == 0) tree.assignMapSymbol(pick(random, "GREEN_CIRCLE", "BLUE_SQUARE", "PURPLE_DIAMOND"), now);
        }
        record WorkTemplate(String name, String description, TreeWorkPriority priority, String symbol) {}
        List<WorkTemplate> templates = List.of(
                new WorkTemplate("Usunięcie posuszu z korony", "Usunięcie suchych gałęzi nad ciągiem pieszym.", TreeWorkPriority.HIGH, "ORANGE_TRIANGLE"),
                new WorkTemplate("Podniesienie korony nad boiskiem", "Skrócenie najniższych gałęzi do skrajni 3 m.", TreeWorkPriority.NORMAL, "ORANGE_TRIANGLE"),
                new WorkTemplate("Redukcja korony o 20%", "Zmniejszenie obciążenia przewodników.", TreeWorkPriority.NORMAL, "ORANGE_TRIANGLE"),
                new WorkTemplate("Montaż wiązania elastycznego korony", "Zabezpieczenie rozwidlenia typu V.", TreeWorkPriority.HIGH, "PURPLE_DIAMOND"),
                new WorkTemplate("Wycinka drzewa", "Drzewo obumierające w strefie zabaw – wycinka po uzyskaniu zgody.", TreeWorkPriority.URGENT, "RED_CROSS"),
                new WorkTemplate("Nasadzenie zastępcze", "Nasadzenie lipy drobnolistnej (obwód 16–18 cm).", TreeWorkPriority.LOW, "GREEN_CIRCLE"),
                new WorkTemplate("Usunięcie odrostów korzeniowych", "Wycięcie odrostów przy ogrodzeniu.", TreeWorkPriority.LOW, null),
                new WorkTemplate("Kontrola po sezonie zimowym", "Kontrola stanu po wichurach.", TreeWorkPriority.NORMAL, "BLUE_SQUARE"));
        int[][] schedule = {
                // {status: 0=wykonana 1=zaplanowana 2=zalecenie 3=anulowana, dni od dzis}
                {0, -28}, {0, -21}, {0, -14}, {0, -9}, {0, -4}, {0, -2},
                {1, -3}, {1, 1}, {1, 1}, {1, 2}, {1, 4}, {1, 5}, {1, 6}, {1, 9}, {1, 12}, {1, 16}, {1, 20}, {1, 27}, {1, 33}, {1, 41},
                {2, 0}, {2, 0}, {2, 0}, {2, 0}, {2, 0}, {2, 0}, {2, 0}, {2, 0},
                {3, -10}};
        for (int index = 0; index < schedule.length; index++) {
            Tree tree = trees.get((index * 7) % trees.size());
            WorkTemplate template = templates.get(index % templates.size());
            int[] entry = schedule[index];
            TreeWorkStatus status = switch (entry[0]) {
                case 0 -> TreeWorkStatus.COMPLETED;
                case 1 -> TreeWorkStatus.PLANNED;
                case 2 -> TreeWorkStatus.RECOMMENDED;
                default -> TreeWorkStatus.CANCELLED;
            };
            Integer plannedInDays = entry[0] == 2 ? null : entry[1];
            Integer completedInDays = entry[0] == 0 ? entry[1] : null;
            addWork(tree, template.name(), template.description(), status, template.priority(),
                    plannedInDays == null ? null : (status == TreeWorkStatus.COMPLETED ? plannedInDays - 3 : plannedInDays),
                    completedInDays, template.symbol());
        }
        for (int index = 0; index < 6; index++) {
            Tree tree = trees.get(index * 5);
            addPhoto(tree, Shot.WHOLE_TREE, false, "Drzewo " + tree.getInventoryNumber() + " przy boisku szkolnym.", true, false);
        }
        createStand(project, trees, "Skarpa przy boisku", "Strefa zabaw i boisko – drzewa o najwyższym priorytecie kontroli.",
                rectangle(centerLongitude - 0.0009, centerLatitude, centerLongitude + 0.0009, centerLatitude + 0.0006));
        createStand(project, trees, "Ogród przy wejściu", "Nasadzenia ozdobne przy wejściu głównym.",
                rectangle(centerLongitude, centerLatitude - 0.0006, centerLongitude + 0.0009, centerLatitude));
        return trees.size();
    }

    // ---------------------------------------------------------------- powiadomienia

    void writeNotifications(UserAccount owner) {
        List<Project> projects = projectRepository.findAllByOwnerIdOrderByCreatedAtDesc(owner.getId());
        for (Project project : projects) {
            for (TreeMonitoringRecord record : treeMonitoringRecordRepository.findAllOfProjectNewestFirst(project.getId())) {
                if (record.getHealthStatus() != TreeHealthStatus.POOR) continue;
                Tree tree = record.getTree();
                String key = "demo-tree-poor-health:" + record.getId();
                if (userNotificationRepository.existsByUserAccountIdAndDeduplicationKey(owner.getId(), key)) continue;
                userNotificationRepository.save(UserNotification.create(owner, UserNotificationType.TREE_POOR_HEALTH,
                        "Drzewo " + tree.getInventoryNumber() + ": stan zły",
                        "Kontrola w projekcie „" + project.getName() + "” oznaczyła stan drzewa jako zły.",
                        "/projects/" + project.getId() + "/trees/" + tree.getId(), key, now));
            }
        }
    }

    // ---------------------------------------------------------------- klocki

    private Project newProject(UserAccount owner, String name, String description, String clientName, String siteAddress,
                               int orderDaysAgo, String contractNumber, String documentPurpose, String legalBasis) {
        Project project = Project.createOwnedBy(owner, name, description + DEMO_DESCRIPTION_SUFFIX, now);
        project.setClientName(clientName);
        project.setSiteAddress(siteAddress);
        project.setOrderDate(today.minusDays(orderDaysAgo));
        project.setContractNumber(contractNumber);
        project.setDocumentPurpose(documentPurpose);
        project.setLegalBasis(legalBasis);
        // kolejne projekty z rosnacym czasem utworzenia - stala kolejnosc na liscie
        now = now.plusSeconds(1);
        return projectRepository.save(project);
    }

    private Tree newTree(Project project, String inventoryNumber, Species species, int circumferenceCm, double latitude,
                         double longitude, TreeLocationSource source, BigDecimal accuracyM, Instant measuredAt, String adapterCode) {
        double height = Math.min(species.maximumHeightM(), 3.5 + circumferenceCm / 15.0 + random.nextDouble() * 3);
        double crown = Math.max(1.5, Math.min(26, circumferenceCm / 26.0 + random.nextDouble() * 2.5));
        String condition = circumferenceCm > 400 ? pick(random, FAIR_CONDITION) : pick(random, GOOD_CONDITION);
        String notes = pick(random, FIELD_NOTES);
        Tree tree = Tree.createInProject(project, new TreeEditableAttributes(inventoryNumber, species.latin(), species.polish(),
                circumferenceCm, decimal(height), decimal(crown), notes.isEmpty() ? null : notes, condition, null), now);
        tree.placeAt(TreeLocation.of(BigDecimal.valueOf(latitude), BigDecimal.valueOf(longitude), decimal(205 + random.nextDouble() * 12),
                accuracyM, source, measuredAt, adapterCode, null, now), now);
        return treeRepository.save(tree);
    }

    private void valuate(Tree tree, TreeValuationCategory category) {
        String age = switch (category) {
            case VERY_HIGH -> "Drzewo ok. 150–200 lat, okaz o cechach pomnikowych.";
            case HIGH -> "Drzewo dojrzałe, ok. 80–120 lat.";
            case MEDIUM -> "Drzewo w średnim wieku, ok. 40–70 lat.";
            case LOW -> "Drzewo młode.";
        };
        treeValuationRepository.save(TreeValuation.createForTree(tree, new TreeValuation.Assessment(category, age,
                category.ordinal() >= 2 ? "Gatunek rodzimy, dobrze przystosowany do siedliska." : null,
                category.ordinal() >= 2 ? "Miejsce bytowania ptaków i owadów." : null,
                category == TreeValuationCategory.VERY_HIGH ? "Element historycznego układu zieleni." : null,
                category.ordinal() >= 1 ? "Ważny element kompozycji przestrzeni." : null,
                "Ocena arborysty na podstawie oględzin i pomiarów (dane przykładowe).", null), now));
    }

    private void inspect(Tree tree, int daysAgo, TreeHealthStatus health, int nextInspectionInDays) {
        String notes = switch (health) {
            case GOOD -> pick(random, GOOD_CONDITION);
            case FAIR -> pick(random, FAIR_CONDITION);
            case POOR -> pick(random, POOR_CONDITION);
        };
        LocalDate inspectionDate = today.minusDays(daysAgo);
        treeMonitoringRecordRepository.save(TreeMonitoringRecord.createForTree(tree, new TreeMonitoringRecord.Fields(
                inspectionDate, health, notes, nextInspectionInDays > 0 ? inspectionDate.plusDays(nextInspectionInDays) : null), now));
        if (health == TreeHealthStatus.POOR) {
            tree.replaceEditableAttributes(new TreeEditableAttributes(tree.getInventoryNumber(), tree.getSpeciesLatin(),
                    tree.getSpeciesPolish(), tree.getTrunkCircumferenceCm(), tree.getHeightM(), tree.getCrownDiameterM(),
                    tree.getNotes(), notes, pick(random, HAZARDS)), now);
            mark(tree, "HAZARD");
        } else if (health == TreeHealthStatus.FAIR && random.nextInt(3) == 0) {
            mark(tree, "OBSERVATION");
        }
    }

    private void planWorksForHealth(Tree tree, TreeHealthStatus health, int plannedInDays) {
        if (health == TreeHealthStatus.POOR) {
            boolean removal = random.nextBoolean();
            addWork(tree, removal ? "Wycinka drzewa" : "Redukcja korony i usunięcie posuszu",
                    removal ? "Drzewo zagraża bezpieczeństwu – wycinka po uzyskaniu zezwolenia."
                            : "Zmniejszenie ryzyka wyłamania konarów nad ciągiem pieszym.",
                    TreeWorkStatus.PLANNED, TreeWorkPriority.URGENT, plannedInDays + random.nextInt(6), null,
                    removal ? "RED_CROSS" : "ORANGE_TRIANGLE");
            if (removal) mark(tree, "TO_REMOVE");
        } else if (health == TreeHealthStatus.FAIR && random.nextBoolean()) {
            addWork(tree, "Cięcia sanitarne", "Usunięcie posuszu i gałęzi uszkodzonych.", TreeWorkStatus.RECOMMENDED,
                    TreeWorkPriority.NORMAL, null, null, "ORANGE_TRIANGLE");
        } else if (random.nextInt(9) == 0) {
            addWork(tree, "Usunięcie posuszu", "Wykonano podczas inwentaryzacji.", TreeWorkStatus.COMPLETED,
                    TreeWorkPriority.LOW, -12, -10, null);
        }
    }

    private void addWork(Tree tree, String name, String description, TreeWorkStatus status, TreeWorkPriority priority,
                         Integer plannedInDays, Integer completedInDays, String mapSymbolCode) {
        TreeWork work = TreeWork.createForTree(tree, new TreeWork.Fields(name, description, status, priority,
                plannedInDays == null ? null : today.plusDays(plannedInDays),
                completedInDays == null ? null : today.plusDays(completedInDays)), now);
        if (mapSymbolCode != null) work.assignMapSymbol(mapSymbolCode, now);
        treeWorkRepository.save(work);
    }

    private final Set<String> markedTrees = new HashSet<>();

    private void mark(Tree tree, String code) {
        if (!markedTrees.add(tree.getId() + ":" + code)) return;
        treeMarkingRepository.save(TreeMarking.create(tree, code, now));
    }

    private void createStand(Project project, List<Tree> trees, String name, String notes, List<double[]> ring) {
        StandPolygon polygon = StandPolygon.ofRings(List.of(ring));
        Set<Tree> treesInside = new HashSet<>();
        for (Tree tree : trees) {
            TreeLocation location = tree.getLocation();
            if (location != null && polygon.contains(location.getLongitude().doubleValue(), location.getLatitude().doubleValue())) {
                treesInside.add(tree);
            }
        }
        treeStandRepository.save(TreeStand.create(project, name, notes, polygon.toGeoJson(objectMapper).toString(),
                polygon.roundedAreaSquareMeters(), TreeStandSource.MANUAL, treesInside, now));
    }

    private void addPhoto(Tree tree, Shot shot, boolean autumn, String caption, boolean includedInDocument, boolean annotate) {
        boolean conifer = isConifer(tree.getSpeciesLatin());
        BufferedImage image = DemoTreeIllustrator.draw(shot, conifer, autumn, random.nextLong());
        byte[] jpeg = DemoTreeIllustrator.toJpeg(image);
        String storageKey = UUID.randomUUID() + ".jpg";
        photoStorage.store(storageKey, new ByteArrayInputStream(jpeg));
        TreePhoto photo = TreePhoto.createForTree(tree, "IMG_%04d.jpg".formatted(1000 + random.nextInt(9000)), storageKey,
                "image/jpeg", jpeg.length, now, null);
        photo.changeDocumentSettings(caption, includedInDocument);
        photo = treePhotoRepository.save(photo);
        if (!annotate) return;

        List<AnnotationStroke> strokes = DemoTreeIllustrator.annotationFor(shot);
        byte[] png = DemoTreeIllustrator.toPng(DemoTreeIllustrator.withAnnotations(image, strokes));
        String renderedStorageKey = UUID.randomUUID() + ".png";
        photoStorage.store(renderedStorageKey, new ByteArrayInputStream(png));
        List<PhotoAnnotationElementDto> elements = new ArrayList<>();
        for (int index = 0; index < strokes.size(); index++) {
            AnnotationStroke stroke = strokes.get(index);
            elements.add(new PhotoAnnotationElementDto("demo-" + (index + 1), stroke.type(),
                    stroke.points().stream().map(point -> new NormalizedPointDto(normalized(point[0]), normalized(point[1]))).toList(),
                    Color.RED.equals(stroke.color()) ? "RED" : "YELLOW", "MEDIUM", null));
        }
        String documentJson = objectMapper.writeValueAsString(
                new PhotoAnnotationDocumentRequest(PhotoAnnotationLimits.CURRENT_SCHEMA_VERSION, elements));
        treePhotoAnnotationRepository.save(TreePhotoAnnotation.createForPhoto(photo, PhotoAnnotationLimits.CURRENT_SCHEMA_VERSION,
                documentJson, renderedStorageKey, "image/png", png.length, now));
    }

    private static boolean isConifer(String speciesLatin) {
        String genus = speciesLatin == null ? "" : speciesLatin.split(" ")[0].toLowerCase();
        return Set.of("pinus", "picea", "larix", "taxus", "thuja", "pseudotsuga", "abies").contains(genus);
    }

    private int randomCircumference(Species species) {
        return species.minimumCircumferenceCm() + random.nextInt(species.maximumCircumferenceCm() - species.minimumCircumferenceCm());
    }

    private static TreeValuationCategory categoryFromCircumference(Tree tree) {
        int circumference = tree.getTrunkCircumferenceCm();
        if (circumference > 330) return TreeValuationCategory.VERY_HIGH;
        if (circumference > 210) return TreeValuationCategory.HIGH;
        if (circumference > 110) return TreeValuationCategory.MEDIUM;
        return TreeValuationCategory.LOW;
    }

    private TreeHealthStatus randomHealth(int goodPercent, int fairPercent) {
        int roll = random.nextInt(100);
        if (roll < goodPercent) return TreeHealthStatus.GOOD;
        if (roll < goodPercent + fairPercent) return TreeHealthStatus.FAIR;
        return TreeHealthStatus.POOR;
    }

    private Instant measurementTime(int daysAgo) {
        return today.minusDays(daysAgo).atTime(9 + random.nextInt(6), random.nextInt(60)).atZone(WARSAW).toInstant();
    }

    private static List<double[]> rectangle(double west, double south, double east, double north) {
        return List.of(new double[]{west, south}, new double[]{east, south}, new double[]{east, north}, new double[]{west, north});
    }

    private static BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal normalized(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }
}

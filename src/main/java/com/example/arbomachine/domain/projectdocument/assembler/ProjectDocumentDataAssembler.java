package com.example.arbomachine.domain.projectdocument.assembler;

import com.example.arbomachine.domain.documentauthor.entity.DocumentAuthorProfile;
import com.example.arbomachine.domain.documentauthor.service.documentauthorprofilefacade.FindDocumentAuthorDetailsOfUserAccountHandler;
import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.projectdocument.data.ProjectDocumentAuthorData;
import com.example.arbomachine.domain.projectdocument.data.ProjectDocumentData;
import com.example.arbomachine.domain.projectdocument.data.ProjectDocumentLocationData;
import com.example.arbomachine.domain.projectdocument.data.ProjectDocumentMonitoringData;
import com.example.arbomachine.domain.projectdocument.data.ProjectDocumentStandData;
import com.example.arbomachine.domain.projectdocument.data.ProjectDocumentPhotoData;
import com.example.arbomachine.domain.projectdocument.data.ProjectDocumentTreeData;
import com.example.arbomachine.domain.projectdocument.data.ProjectDocumentWorkData;
import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.treephoto.entity.TreePhoto;
import com.example.arbomachine.domain.treephoto.entity.TreePhotoAnnotation;
import com.example.arbomachine.domain.treephoto.storage.PhotoStorage;
import com.example.arbomachine.domain.treework.entity.TreeWork;
import com.example.arbomachine.infrastructure.repository.TreeMonitoringRecordRepository;
import com.example.arbomachine.infrastructure.repository.TreeStandRepository;
import com.example.arbomachine.infrastructure.repository.TreePhotoAnnotationRepository;
import com.example.arbomachine.infrastructure.repository.TreePhotoRepository;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import com.example.arbomachine.infrastructure.repository.TreeValuationRepository;
import com.example.arbomachine.infrastructure.repository.TreeWorkRepository;
import com.example.arbomachine.domain.projectdocument.data.ProjectDocumentValuationData;
import com.example.arbomachine.domain.treevaluation.entity.TreeValuation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import com.example.arbomachine.domain.treemarking.catalog.TreeMarkingCatalog;
import com.example.arbomachine.infrastructure.repository.TreeMarkingRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Sklada ProjectDocumentData z danych WLASNEGO projektu (wlasnosc sprawdza wczesniej handler). Stala liczba zapytan
 * niezaleznie od liczby drzew: drzewa, waloryzacje, prace projektu, zdjecia projektu, adnotacje tych zdjec, kontrole stanu,
 * oznaczenia drzew (etap 31), dane autora z profilu wlasciciela (etap 42) - laczone w pamieci
 * (brak N+1). Plikow zdjec NIE czyta - daje tylko leniwy dostep do tresci (wersja z adnotacja, jesli istnieje,
 * jak .../documentation-content). Kolejnosc: drzewa, prace i zdjecia w kolejnosci dodania.
 */
@Component
public class ProjectDocumentDataAssembler {

    /** Formaty, ktore generator PDF potrafi osadzic (WebP nie jest obslugiwany przez ImageIO bez dodatkow). */
    private static final Set<String> EMBEDDABLE_CONTENT_TYPES = Set.of("image/jpeg", "image/png");

    private final TreeRepository treeRepository;
    private final TreeWorkRepository treeWorkRepository;
    private final TreePhotoRepository treePhotoRepository;
    private final TreePhotoAnnotationRepository treePhotoAnnotationRepository;
    private final PhotoStorage photoStorage;
    private final TreeValuationRepository treeValuationRepository;
    private final TreeMonitoringRecordRepository treeMonitoringRecordRepository;
    private final TreeStandRepository treeStandRepository;
    private final TreeMarkingRepository treeMarkingRepository;
    private final TreeMarkingCatalog treeMarkingCatalog;
    private final FindDocumentAuthorDetailsOfUserAccountHandler findDocumentAuthorDetailsOfUserAccountHandler;

    public ProjectDocumentDataAssembler(TreeRepository treeRepository, TreeWorkRepository treeWorkRepository,
                                        TreePhotoRepository treePhotoRepository,
                                        TreePhotoAnnotationRepository treePhotoAnnotationRepository,
                                        PhotoStorage photoStorage,
                                        TreeValuationRepository treeValuationRepository,
                                        TreeMonitoringRecordRepository treeMonitoringRecordRepository,
                                        TreeStandRepository treeStandRepository, TreeMarkingRepository treeMarkingRepository,
                                        TreeMarkingCatalog treeMarkingCatalog,
                                        FindDocumentAuthorDetailsOfUserAccountHandler findDocumentAuthorDetailsOfUserAccountHandler) {
        this.treeRepository = treeRepository;
        this.treeWorkRepository = treeWorkRepository;
        this.treePhotoRepository = treePhotoRepository;
        this.treePhotoAnnotationRepository = treePhotoAnnotationRepository;
        this.photoStorage = photoStorage;
        this.treeValuationRepository = treeValuationRepository;
        this.treeMonitoringRecordRepository = treeMonitoringRecordRepository;
        this.treeStandRepository = treeStandRepository;
        this.treeMarkingRepository = treeMarkingRepository;
        this.treeMarkingCatalog = treeMarkingCatalog;
        this.findDocumentAuthorDetailsOfUserAccountHandler = findDocumentAuthorDetailsOfUserAccountHandler;
    }

    @Transactional(readOnly = true)
    public ProjectDocumentData assemble(Project ownedProject, Instant generatedAt) {
        List<Tree> trees = treeRepository.findAllByProjectIdOrderByCreatedAtAscIdAsc(ownedProject.getId());
        Map<Long, List<TreeWork>> worksByTreeId = treeWorkRepository
                .findAllByTreeProjectIdOrderByTreeIdAscCreatedAtAscIdAsc(ownedProject.getId()).stream()
                .collect(Collectors.groupingBy(work -> work.getTree().getId()));
        List<TreePhoto> photos = treePhotoRepository.findAllOfProjectWithTree(ownedProject.getId());
        Map<Long, List<TreePhoto>> photosByTreeId = photos.stream()
                .collect(Collectors.groupingBy(photo -> photo.getTree().getId()));
        Map<Long, TreePhotoAnnotation> annotationByPhotoId = photos.isEmpty() ? Map.of()
                : treePhotoAnnotationRepository.findAllByTreePhotoIdIn(photos.stream().map(TreePhoto::getId).toList())
                .stream().collect(Collectors.toMap(annotation -> annotation.getTreePhoto().getId(), Function.identity()));

        Map<Long, TreeValuation> valuationByTreeId = treeValuationRepository.findAllByTreeProjectId(ownedProject.getId()).stream()
                .collect(Collectors.toMap(valuation -> valuation.getTree().getId(), Function.identity()));

        Map<Long, List<ProjectDocumentMonitoringData>> monitoringByTreeId = treeMonitoringRecordRepository
                .findAllOfProjectNewestFirst(ownedProject.getId()).stream()
                .collect(Collectors.groupingBy(record -> record.getTree().getId(), Collectors.mapping(
                        record -> new ProjectDocumentMonitoringData(record.getInspectionDate(),
                                record.getHealthStatus() == null ? null : record.getHealthStatus().name(), record.getNotes()),
                        Collectors.toList())));

        Map<Long, List<String>> markingCodesByTreeId = treeMarkingRepository.findTreeIdsAndCodesByProjectId(ownedProject.getId()).stream()
                .collect(Collectors.groupingBy(TreeMarkingRepository.TreeIdAndCode::getTreeId,
                        Collectors.mapping(TreeMarkingRepository.TreeIdAndCode::getCode, Collectors.toList())));

        int[] nextPhotoNumber = {1};
        List<ProjectDocumentTreeData> treeData = new ArrayList<>(trees.size());
        for (Tree tree : trees) {
            List<ProjectDocumentPhotoData> treePhotoData = photosByTreeId.getOrDefault(tree.getId(), List.of()).stream()
                    .map(photo -> photoDataOf(photo, annotationByPhotoId.get(photo.getId()), nextPhotoNumber[0]++))
                    .toList();
            List<ProjectDocumentWorkData> treeWorkData = worksByTreeId.getOrDefault(tree.getId(), List.of()).stream()
                    .map(work -> new ProjectDocumentWorkData(work.getName(), work.getDescription(), work.getStatus().name(),
                            work.getPriority() == null ? null : work.getPriority().name(), work.getPlannedDate(), work.getCompletedDate()))
                    .toList();
            treeData.add(new ProjectDocumentTreeData(tree.getId(), tree.getInventoryNumber(), tree.getSpeciesPolish(),
                    tree.getSpeciesLatin(), tree.getTrunkCircumferenceCm(), tree.getHeightM(), tree.getCrownDiameterM(),
                    tree.getNotes(), tree.getConditionNotes(), tree.getHazardNotes(),
                    tree.getLocation() == null ? null
                            : new ProjectDocumentLocationData(tree.getLocation().getLatitude(), tree.getLocation().getLongitude(),
                            tree.getLocation().getAccuracyM(),
                            tree.getLocation().getSource() == null ? null : tree.getLocation().getSource().name()),
                    valuationDataOf(valuationByTreeId.get(tree.getId())), treeWorkData, treePhotoData,
                    monitoringByTreeId.getOrDefault(tree.getId(), List.of()),
                    treeMarkingCatalog.displayNamesInCatalogOrder(markingCodesByTreeId.getOrDefault(tree.getId(), List.of()))));
        }
        return new ProjectDocumentData(ownedProject.getId(), ownedProject.getName(), ownedProject.getDescription(),
                ownedProject.getClientName(), ownedProject.getSiteAddress(), ownedProject.getOrderDate(),
                ownedProject.getCreatedAt(), ownedProject.getUpdatedAt(), generatedAt, treeData,
                treeStandRepository.findAllByProjectIdOrderByNameAscIdAsc(ownedProject.getId()).stream()
                        .map(stand -> new ProjectDocumentStandData(stand.getName(), stand.getAreaSquareMeters(),
                                stand.getTrees().stream().sorted(java.util.Comparator.comparing(Tree::getId)).map(Tree::getInventoryNumber).toList(),
                                stand.getNotes()))
                        .sorted(java.util.Comparator.comparing(ProjectDocumentStandData::name, NaturalTextOrder.INSTANCE))
                        .toList(),
                findDocumentAuthorDetailsOfUserAccountHandler.handle(ownedProject.getOwner().getId())
                        .map(ProjectDocumentDataAssembler::authorDataOf).orElse(null));
    }

    private static ProjectDocumentAuthorData authorDataOf(DocumentAuthorProfile.Details details) {
        return new ProjectDocumentAuthorData(details.authorFullName(), details.qualifications(), details.companyName(),
                details.companyDetails(), details.methodsDescription());
    }

    private static ProjectDocumentValuationData valuationDataOf(TreeValuation valuation) {
        if (valuation == null) return null;
        return new ProjectDocumentValuationData(
                valuation.getOverallCategory() == null ? null : valuation.getOverallCategory().name(),
                valuation.getAgeValue(), valuation.getNaturalValue(), valuation.getEcologicalValue(),
                valuation.getCulturalValue(), valuation.getLandscapeValue(), valuation.getJustification(), valuation.getNotes());
    }

    private ProjectDocumentPhotoData photoDataOf(TreePhoto photo, TreePhotoAnnotation annotationOrNull, int documentPhotoNumber) {
        if (annotationOrNull != null) {
            String renderedStorageKey = annotationOrNull.getRenderedStorageKey();
            return new ProjectDocumentPhotoData(documentPhotoNumber, photo.getOriginalFileName(), true, true,
                    () -> photoStorage.load(renderedStorageKey).getInputStream());
        }
        String storageKey = photo.getStorageKey();
        return new ProjectDocumentPhotoData(documentPhotoNumber, photo.getOriginalFileName(), false,
                EMBEDDABLE_CONTENT_TYPES.contains(photo.getContentType()),
                () -> photoStorage.load(storageKey).getInputStream());
    }
}

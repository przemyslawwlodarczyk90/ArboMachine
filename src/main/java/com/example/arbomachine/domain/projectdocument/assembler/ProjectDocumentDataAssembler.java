package com.example.arbomachine.domain.projectdocument.assembler;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.projectdocument.data.ProjectDocumentData;
import com.example.arbomachine.domain.projectdocument.data.ProjectDocumentLocationData;
import com.example.arbomachine.domain.projectdocument.data.ProjectDocumentPhotoData;
import com.example.arbomachine.domain.projectdocument.data.ProjectDocumentTreeData;
import com.example.arbomachine.domain.projectdocument.data.ProjectDocumentWorkData;
import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.treephoto.entity.TreePhoto;
import com.example.arbomachine.domain.treephoto.entity.TreePhotoAnnotation;
import com.example.arbomachine.domain.treephoto.storage.PhotoStorage;
import com.example.arbomachine.domain.treework.entity.TreeWork;
import com.example.arbomachine.infrastructure.repository.TreePhotoAnnotationRepository;
import com.example.arbomachine.infrastructure.repository.TreePhotoRepository;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import com.example.arbomachine.infrastructure.repository.TreeWorkRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Sklada ProjectDocumentData z danych WLASNEGO projektu (wlasnosc sprawdza wczesniej handler). Stala liczba zapytan
 * niezaleznie od liczby drzew: drzewa, prace projektu, zdjecia projektu, adnotacje tych zdjec - laczone w pamieci
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

    public ProjectDocumentDataAssembler(TreeRepository treeRepository, TreeWorkRepository treeWorkRepository,
                                        TreePhotoRepository treePhotoRepository,
                                        TreePhotoAnnotationRepository treePhotoAnnotationRepository,
                                        PhotoStorage photoStorage) {
        this.treeRepository = treeRepository;
        this.treeWorkRepository = treeWorkRepository;
        this.treePhotoRepository = treePhotoRepository;
        this.treePhotoAnnotationRepository = treePhotoAnnotationRepository;
        this.photoStorage = photoStorage;
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

        int[] nextPhotoNumber = {1};
        List<ProjectDocumentTreeData> treeData = new ArrayList<>(trees.size());
        for (Tree tree : trees) {
            List<ProjectDocumentPhotoData> treePhotoData = photosByTreeId.getOrDefault(tree.getId(), List.of()).stream()
                    .map(photo -> photoDataOf(photo, annotationByPhotoId.get(photo.getId()), nextPhotoNumber[0]++))
                    .toList();
            List<ProjectDocumentWorkData> treeWorkData = worksByTreeId.getOrDefault(tree.getId(), List.of()).stream()
                    .map(work -> new ProjectDocumentWorkData(work.getName(), work.getDescription()))
                    .toList();
            treeData.add(new ProjectDocumentTreeData(tree.getId(), tree.getInventoryNumber(), tree.getSpeciesPolish(),
                    tree.getSpeciesLatin(), tree.getTrunkCircumferenceCm(), tree.getHeightM(), tree.getCrownDiameterM(),
                    tree.getNotes(), tree.getConditionNotes(), tree.getHazardNotes(),
                    tree.getLocation() == null ? null
                            : new ProjectDocumentLocationData(tree.getLocation().getLatitude(), tree.getLocation().getLongitude()),
                    treeWorkData, treePhotoData));
        }
        return new ProjectDocumentData(ownedProject.getId(), ownedProject.getName(), ownedProject.getDescription(),
                ownedProject.getCreatedAt(), ownedProject.getUpdatedAt(), generatedAt, treeData);
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

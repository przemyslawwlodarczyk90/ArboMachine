package com.example.arbomachine.domain.treephoto.cleanup;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.service.treefacade.TreeDataCleaner;
import com.example.arbomachine.domain.treephoto.entity.TreePhoto;
import com.example.arbomachine.domain.treephoto.entity.TreePhotoAnnotation;
import com.example.arbomachine.domain.treephoto.storage.PhotoStorage;
import com.example.arbomachine.infrastructure.repository.TreePhotoAnnotationRepository;
import com.example.arbomachine.infrastructure.repository.TreePhotoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Usuwane drzewo traci zdjecia i adnotacje (etap 44): rekordy w transakcji usuwania drzewa, pliki (oryginaly
 * i splaszczone PNG) dopiero PO commicie - wycofana transakcja nie zostawia rekordow bez plikow. Pliku, ktorego nie
 * da sie usunac, nie ponawiamy (osierocony plik w logu - jak przy usuwaniu pojedynczego zdjecia).
 */
@Component
public class TreePhotoTreeDataCleaner implements TreeDataCleaner {
    private static final Logger LOGGER = LoggerFactory.getLogger(TreePhotoTreeDataCleaner.class);

    private final TreePhotoRepository treePhotoRepository;
    private final TreePhotoAnnotationRepository treePhotoAnnotationRepository;
    private final PhotoStorage photoStorage;

    public TreePhotoTreeDataCleaner(TreePhotoRepository treePhotoRepository,
                                    TreePhotoAnnotationRepository treePhotoAnnotationRepository, PhotoStorage photoStorage) {
        this.treePhotoRepository = treePhotoRepository;
        this.treePhotoAnnotationRepository = treePhotoAnnotationRepository;
        this.photoStorage = photoStorage;
    }

    @Override
    public void deleteDataOf(Tree treeToDelete) {
        List<TreePhoto> photos = treePhotoRepository.findAllByTreeIdOrderByCreatedAtAscIdAsc(treeToDelete.getId());
        if (photos.isEmpty()) return;
        List<TreePhotoAnnotation> annotations = treePhotoAnnotationRepository
                .findAllByTreePhotoIdIn(photos.stream().map(TreePhoto::getId).toList());
        List<String> storageKeysToDelete = new ArrayList<>();
        annotations.forEach(annotation -> storageKeysToDelete.add(annotation.getRenderedStorageKey()));
        photos.forEach(photo -> storageKeysToDelete.add(photo.getStorageKey()));
        treePhotoAnnotationRepository.deleteAll(annotations);
        treePhotoRepository.deleteAll(photos);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteFiles(treeToDelete.getId(), storageKeysToDelete);
            }
        });
    }

    private void deleteFiles(Long deletedTreeId, List<String> storageKeys) {
        for (String storageKey : storageKeys) {
            try {
                photoStorage.delete(storageKey);
            } catch (RuntimeException fileDeletionError) {
                LOGGER.warn("Tree {} deleted, but photo file {} could not be deleted (orphaned file)",
                        deletedTreeId, storageKey, fileDeletionError);
            }
        }
    }
}

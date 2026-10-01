package com.example.arbomachine.domain.treephoto.service.treephotoannotationfacade;

import com.example.arbomachine.domain.treephoto.annotation.PhotoAnnotationLimits;
import com.example.arbomachine.domain.treephoto.config.TreePhotoProperties;
import com.example.arbomachine.domain.treephoto.dto.PhotoAnnotationDocumentRequest;
import com.example.arbomachine.domain.treephoto.dto.PhotoAnnotationResponse;
import com.example.arbomachine.domain.treephoto.dto.RenderedAnnotationImageUpload;
import com.example.arbomachine.domain.treephoto.entity.TreePhoto;
import com.example.arbomachine.domain.treephoto.entity.TreePhotoAnnotation;
import com.example.arbomachine.domain.treephoto.exception.InvalidRenderedAnnotationImageException;
import com.example.arbomachine.domain.treephoto.exception.TreePhotoFileTooLargeException;
import com.example.arbomachine.domain.treephoto.service.treephotofacade.FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.treephoto.storage.PhotoStorage;
import com.example.arbomachine.domain.treephoto.storage.PhotoStorageException;
import com.example.arbomachine.infrastructure.repository.TreePhotoAnnotationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/**
 * Utworzenie albo zastapienie adnotacji zdjecia (PUT). Oryginalne zdjecie nie jest dotykane.
 * Kolejnosc (baza i pliki to nie jedna transakcja):
 * 1. wlasnosc (404) i walidacja dokumentu (400) oraz PNG (415/413) - nic jeszcze nie zapisano,
 * 2. NOWY splaszczony PNG pod nowym kluczem UUID - blad storage = baza bez zmian,
 * 3. zapis metadanych - blad bazy = usuniecie nowego pliku (kompensacja),
 * 4. dopiero po udanym przelaczeniu - best effort usuniecie starego PNG.
 * Nigdy odwrotnie: osierocony stary plik jest bezpieczniejszy niz utrata aktualnej dokumentacji.
 */
@Component
public class SaveTreePhotoAnnotationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(SaveTreePhotoAnnotationHandler.class);
    private static final String PNG_CONTENT_TYPE = "image/png";
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    private final FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler findTreePhotoHandler;
    private final TreePhotoAnnotationRepository treePhotoAnnotationRepository;
    private final PhotoStorage photoStorage;
    private final TreePhotoProperties treePhotoProperties;
    private final ObjectMapper objectMapper;

    public SaveTreePhotoAnnotationHandler(FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler findTreePhotoHandler,
                                          TreePhotoAnnotationRepository treePhotoAnnotationRepository,
                                          PhotoStorage photoStorage, TreePhotoProperties treePhotoProperties,
                                          ObjectMapper objectMapper) {
        this.findTreePhotoHandler = findTreePhotoHandler;
        this.treePhotoAnnotationRepository = treePhotoAnnotationRepository;
        this.photoStorage = photoStorage;
        this.treePhotoProperties = treePhotoProperties;
        this.objectMapper = objectMapper;
    }

    public PhotoAnnotationResponse handle(String currentUserEmail, Long projectId, Long treeId, Long treePhotoId,
                                          PhotoAnnotationDocumentRequest document, RenderedAnnotationImageUpload renderedImage) {
        TreePhoto treePhoto = findTreePhotoHandler.handle(currentUserEmail, projectId, treeId, treePhotoId);
        PhotoAnnotationDocumentRequest validatedDocument = PhotoAnnotationDocumentValidator.validatedDocumentOf(document);
        validateRenderedImage(renderedImage);
        String documentJson = TreePhotoAnnotationMapper.toDocumentJson(objectMapper, validatedDocument);

        String newRenderedStorageKey = UUID.randomUUID() + ".png";
        try (InputStream renderedContent = renderedImage.content().getInputStream()) {
            photoStorage.store(newRenderedStorageKey, renderedContent);
        } catch (IOException readingError) {
            throw new PhotoStorageException("Could not read rendered annotation image", readingError);
        }

        Optional<TreePhotoAnnotation> existingAnnotation = treePhotoAnnotationRepository.findByTreePhotoId(treePhoto.getId());
        String previousRenderedStorageKey = existingAnnotation.map(TreePhotoAnnotation::getRenderedStorageKey).orElse(null);
        TreePhotoAnnotation savedAnnotation;
        try {
            Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
            TreePhotoAnnotation annotation = existingAnnotation.orElse(null);
            if (annotation == null) {
                annotation = TreePhotoAnnotation.createForPhoto(treePhoto, PhotoAnnotationLimits.CURRENT_SCHEMA_VERSION,
                        documentJson, newRenderedStorageKey, PNG_CONTENT_TYPE, renderedImage.fileSizeBytes(), now);
            } else {
                annotation.replaceDocument(PhotoAnnotationLimits.CURRENT_SCHEMA_VERSION, documentJson, newRenderedStorageKey,
                        PNG_CONTENT_TYPE, renderedImage.fileSizeBytes(), now);
            }
            savedAnnotation = treePhotoAnnotationRepository.save(annotation);
        } catch (RuntimeException metadataSavingError) {
            deleteQuietly(newRenderedStorageKey, metadataSavingError);
            throw metadataSavingError;
        }

        if (previousRenderedStorageKey != null) {
            try {
                photoStorage.delete(previousRenderedStorageKey);
            } catch (RuntimeException oldFileDeletionError) {
                LOGGER.warn("Previous rendered annotation {} could not be deleted (orphaned file)",
                        previousRenderedStorageKey, oldFileDeletionError);
            }
        }
        return TreePhotoAnnotationMapper.toResponse(objectMapper, savedAnnotation);
    }

    private void validateRenderedImage(RenderedAnnotationImageUpload renderedImage) {
        if (renderedImage == null || renderedImage.fileSizeBytes() <= 0) {
            throw new InvalidRenderedAnnotationImageException("Rendered annotation image must be a PNG");
        }
        if (renderedImage.fileSizeBytes() > treePhotoProperties.maxFileSize().toBytes()) {
            throw new TreePhotoFileTooLargeException("Photo file is too large");
        }
        if (!PNG_CONTENT_TYPE.equalsIgnoreCase(String.valueOf(renderedImage.declaredContentType()).trim())) {
            throw new InvalidRenderedAnnotationImageException("Rendered annotation image must be a PNG");
        }
        try (InputStream renderedContent = renderedImage.content().getInputStream()) {
            if (!Arrays.equals(renderedContent.readNBytes(PNG_SIGNATURE.length), PNG_SIGNATURE)) {
                throw new InvalidRenderedAnnotationImageException("Rendered annotation image must be a PNG");
            }
        } catch (IOException readingError) {
            throw new PhotoStorageException("Could not read rendered annotation image", readingError);
        }
    }

    private void deleteQuietly(String renderedStorageKey, RuntimeException metadataSavingError) {
        try {
            photoStorage.delete(renderedStorageKey);
        } catch (RuntimeException cleanupError) {
            metadataSavingError.addSuppressed(cleanupError);
            LOGGER.error("Rendered annotation {} left without metadata after failed save", renderedStorageKey, cleanupError);
        }
    }
}

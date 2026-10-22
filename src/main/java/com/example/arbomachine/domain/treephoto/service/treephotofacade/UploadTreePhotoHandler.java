package com.example.arbomachine.domain.treephoto.service.treephotofacade;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.service.treefacade.FindTreeInProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.treephoto.config.TreePhotoProperties;
import com.example.arbomachine.domain.treephoto.dto.TreePhotoResponse;
import com.example.arbomachine.domain.treephoto.dto.TreePhotoUpload;
import com.example.arbomachine.domain.treephoto.entity.TreePhoto;
import com.example.arbomachine.domain.treephoto.exception.InvalidClientUploadIdException;
import com.example.arbomachine.domain.treephoto.exception.TreePhotoFileMissingException;
import com.example.arbomachine.domain.treephoto.exception.TreePhotoFileTooLargeException;
import com.example.arbomachine.domain.treephoto.exception.UnsupportedTreePhotoTypeException;
import com.example.arbomachine.domain.treephoto.mapper.TreePhotoMapper;
import com.example.arbomachine.domain.treephoto.storage.PhotoStorage;
import com.example.arbomachine.domain.treephoto.storage.PhotoStorageException;
import com.example.arbomachine.infrastructure.repository.TreePhotoAnnotationRepository;
import com.example.arbomachine.infrastructure.repository.TreePhotoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Dodanie zdjecia do drzewa w WLASNYM projekcie. Kolejnosc: wlasnosc (404) -> walidacja pliku (brak, rozmiar,
 * typ z zawartosci) -> zapis pliku w storage -> zapis metadanych w bazie.
 * <p>
 * Baza i storage to nie jedna transakcja: jesli zapis metadanych sie nie uda, plik jest usuwany (sprzatanie),
 * zeby nie zostawal plik bez rekordu. Handler celowo nie jest @Transactional - zapis encji commituje sie
 * w repozytorium, wiec blad bazy widac tutaj, zanim odpowiedz trafi do klienta.
 * <p>
 * Etap 33 (zdjecia offline): z clientUploadId upload jest idempotentny - zdjecie juz zapisane z tym id w tym drzewie
 * jest zwracane bez zapisu pliku; dwa rownolegle takie same uploady rozstrzyga unikalnosc (tree_id, client_upload_id).
 */
@Component
public class UploadTreePhotoHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(UploadTreePhotoHandler.class);

    /** Nazwa zastepcza, gdy przegladarka nie podala nazwy pliku. */
    private static final String FALLBACK_ORIGINAL_FILE_NAME = "photo";

    private final FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler;
    private final TreePhotoRepository treePhotoRepository;
    private final PhotoStorage photoStorage;
    private final TreePhotoMapper treePhotoMapper;
    private final TreePhotoProperties treePhotoProperties;
    private final TreePhotoAnnotationRepository treePhotoAnnotationRepository;

    public UploadTreePhotoHandler(FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler,
                                  TreePhotoRepository treePhotoRepository, PhotoStorage photoStorage,
                                  TreePhotoMapper treePhotoMapper, TreePhotoProperties treePhotoProperties,
                                  TreePhotoAnnotationRepository treePhotoAnnotationRepository) {
        this.findTreeInProjectOwnedByCurrentUserHandler = findTreeInProjectOwnedByCurrentUserHandler;
        this.treePhotoRepository = treePhotoRepository;
        this.photoStorage = photoStorage;
        this.treePhotoMapper = treePhotoMapper;
        this.treePhotoProperties = treePhotoProperties;
        this.treePhotoAnnotationRepository = treePhotoAnnotationRepository;
    }

    public TreePhotoResponse handle(String currentUserEmail, Long projectId, Long treeId, TreePhotoUpload treePhotoUpload) {
        Tree treeInOwnedProject = findTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId, treeId);
        String clientUploadId = validatedClientUploadIdOf(treePhotoUpload.clientUploadId());
        if (clientUploadId != null) {
            Optional<TreePhoto> alreadyUploadedPhoto = treePhotoRepository
                    .findByTreeIdAndClientUploadId(treeInOwnedProject.getId(), clientUploadId);
            if (alreadyUploadedPhoto.isPresent()) return responseOf(alreadyUploadedPhoto.get());
        }
        SupportedTreePhotoType photoType = validatedPhotoTypeOf(treePhotoUpload);

        // Fizyczna nazwa pliku nigdy nie pochodzi od uzytkownika - UUID wyklucza "../", nadpisanie i kolizje nazw.
        String storageKey = UUID.randomUUID() + "." + photoType.fileExtension();
        try (InputStream photoContent = treePhotoUpload.content().getInputStream()) {
            photoStorage.store(storageKey, photoContent);
        } catch (IOException readingError) {
            throw new PhotoStorageException("Could not read uploaded photo", readingError);
        }

        try {
            TreePhoto savedTreePhoto = treePhotoRepository.save(TreePhoto.createForTree(treeInOwnedProject,
                    displayableOriginalFileNameOf(treePhotoUpload.originalFileName()), storageKey,
                    photoType.contentType(), treePhotoUpload.fileSizeBytes(),
                    Instant.now().truncatedTo(ChronoUnit.MICROS), clientUploadId));
            return treePhotoMapper.toTreePhotoResponse(savedTreePhoto, null);
        } catch (DataIntegrityViolationException duplicateUpload) {
            // Rownolegly upload z tym samym clientUploadId zdazyl zapisac rekord - nasz plik jest zbedny.
            deleteStoredFileAfterFailedSave(storageKey, duplicateUpload);
            if (clientUploadId == null) throw duplicateUpload;
            return treePhotoRepository.findByTreeIdAndClientUploadId(treeInOwnedProject.getId(), clientUploadId)
                    .map(this::responseOf)
                    .orElseThrow(() -> duplicateUpload);
        } catch (RuntimeException metadataSavingError) {
            deleteStoredFileAfterFailedSave(storageKey, metadataSavingError);
            throw metadataSavingError;
        }
    }

    private TreePhotoResponse responseOf(TreePhoto treePhoto) {
        return treePhotoMapper.toTreePhotoResponse(treePhoto,
                treePhotoAnnotationRepository.findByTreePhotoId(treePhoto.getId()).orElse(null));
    }

    /** null albo pusty = upload bez identyfikatora; cokolwiek innego musi byc UUID (zapisywany malymi literami). */
    private static String validatedClientUploadIdOf(String clientUploadId) {
        if (clientUploadId == null || clientUploadId.isBlank()) return null;
        try {
            return UUID.fromString(clientUploadId.trim()).toString();
        } catch (IllegalArgumentException notUuid) {
            throw new InvalidClientUploadIdException("Client upload id must be a UUID");
        }
    }

    private SupportedTreePhotoType validatedPhotoTypeOf(TreePhotoUpload treePhotoUpload) {
        if (treePhotoUpload.fileSizeBytes() <= 0) {
            throw new TreePhotoFileMissingException("Photo file is mandatory");
        }
        if (treePhotoUpload.fileSizeBytes() > treePhotoProperties.maxFileSize().toBytes()) {
            throw new TreePhotoFileTooLargeException("Photo file is too large");
        }
        SupportedTreePhotoType declaredPhotoType = SupportedTreePhotoType
                .ofDeclaredContentType(treePhotoUpload.declaredContentType())
                .orElseThrow(() -> new UnsupportedTreePhotoTypeException("Unsupported photo type"));
        if (!declaredPhotoType.matchesSignatureOf(fileStartOf(treePhotoUpload))) {
            throw new UnsupportedTreePhotoTypeException("Unsupported photo type");
        }
        return declaredPhotoType;
    }

    private static byte[] fileStartOf(TreePhotoUpload treePhotoUpload) {
        try (InputStream photoContent = treePhotoUpload.content().getInputStream()) {
            return photoContent.readNBytes(SupportedTreePhotoType.SIGNATURE_LENGTH);
        } catch (IOException readingError) {
            throw new PhotoStorageException("Could not read uploaded photo", readingError);
        }
    }

    /**
     * Nazwa do wyswietlenia: bez katalogow (niektore przegladarki wysylaja pelna sciezke), bez znakow sterujacych,
     * przycieta do dlugosci kolumny. Nie sluzy do budowania zadnej sciezki.
     */
    private static String displayableOriginalFileNameOf(String originalFileName) {
        if (originalFileName == null) return FALLBACK_ORIGINAL_FILE_NAME;
        String fileNameWithoutDirectories = originalFileName.substring(
                Math.max(originalFileName.lastIndexOf('/'), originalFileName.lastIndexOf('\\')) + 1);
        String printableFileName = fileNameWithoutDirectories.replaceAll("\\p{Cntrl}", "").trim();
        if (printableFileName.isEmpty()) return FALLBACK_ORIGINAL_FILE_NAME;
        return printableFileName.length() > TreePhoto.MAXIMUM_ORIGINAL_FILE_NAME_LENGTH
                ? printableFileName.substring(0, TreePhoto.MAXIMUM_ORIGINAL_FILE_NAME_LENGTH)
                : printableFileName;
    }

    private void deleteStoredFileAfterFailedSave(String storageKey, RuntimeException metadataSavingError) {
        try {
            photoStorage.delete(storageKey);
        } catch (RuntimeException cleanupError) {
            metadataSavingError.addSuppressed(cleanupError);
            LOGGER.error("Photo file {} left without metadata after failed save", storageKey, cleanupError);
        }
    }
}

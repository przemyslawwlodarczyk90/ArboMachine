package com.example.arbomachine.domain.treephoto.service.treephotoannotationfacade;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.treephoto.config.TreePhotoProperties;
import com.example.arbomachine.domain.treephoto.dto.NormalizedPointDto;
import com.example.arbomachine.domain.treephoto.dto.PhotoAnnotationDocumentRequest;
import com.example.arbomachine.domain.treephoto.dto.PhotoAnnotationElementDto;
import com.example.arbomachine.domain.treephoto.dto.RenderedAnnotationImageUpload;
import com.example.arbomachine.domain.treephoto.entity.TreePhoto;
import com.example.arbomachine.domain.treephoto.entity.TreePhotoAnnotation;
import com.example.arbomachine.domain.treephoto.service.treephotofacade.FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.treephoto.storage.PhotoStorage;
import com.example.arbomachine.infrastructure.repository.TreePhotoAnnotationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.unit.DataSize;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Spojnosc baza/pliki przy zapisie adnotacji: gdy zapis metadanych w bazie sie nie uda, nowo zapisany PNG
 * jest usuwany (kompensacja), a stary PNG poprzedniej adnotacji zostaje nietkniety.
 */
class SaveTreePhotoAnnotationHandlerTest {

    private static final byte[] PNG_BYTES = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 1, 2, 3};

    private final FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler findTreePhotoHandler =
            mock(FindTreePhotoOfTreeInProjectOwnedByCurrentUserHandler.class);
    private final TreePhotoAnnotationRepository treePhotoAnnotationRepository = mock(TreePhotoAnnotationRepository.class);
    private final PhotoStorage photoStorage = mock(PhotoStorage.class);
    private final SaveTreePhotoAnnotationHandler saveTreePhotoAnnotationHandler = new SaveTreePhotoAnnotationHandler(
            findTreePhotoHandler, treePhotoAnnotationRepository, photoStorage,
            new TreePhotoProperties(DataSize.ofMegabytes(1), Path.of("unused")), new ObjectMapper());

    private final PhotoAnnotationDocumentRequest validDocument = new PhotoAnnotationDocumentRequest(1, List.of(
            new PhotoAnnotationElementDto("l1", "LINE", List.of(new NormalizedPointDto(BigDecimal.ZERO, BigDecimal.ZERO),
                    new NormalizedPointDto(BigDecimal.ONE, BigDecimal.ONE)), "RED", "SMALL", null)));
    private final RenderedAnnotationImageUpload renderedImage =
            new RenderedAnnotationImageUpload("image/png", PNG_BYTES.length, new ByteArrayResource(PNG_BYTES));

    @Test
    void failedMetadataSaveDeletesNewlyStoredRenderedFileAndKeepsPreviousOne() {
        TreePhoto treePhoto = TreePhoto.createForTree(mock(Tree.class), "a.jpg", "0f8fad5b-d9cb-469f-a165-70867728950e.jpg",
                "image/jpeg", 10, Instant.now());
        TreePhotoAnnotation previousAnnotation = TreePhotoAnnotation.createForPhoto(treePhoto, 1, "{}",
                "1b4e28ba-2fa1-11d2-883f-0016d3cca427.png", "image/png", 5, Instant.now());
        when(findTreePhotoHandler.handle(anyString(), any(), any(), any())).thenReturn(treePhoto);
        when(treePhotoAnnotationRepository.findByTreePhotoId(any())).thenReturn(Optional.of(previousAnnotation));
        when(treePhotoAnnotationRepository.save(any())).thenThrow(new RuntimeException("database is down"));

        assertThatThrownBy(() -> saveTreePhotoAnnotationHandler.handle("owner@example.com", 1L, 2L, 3L, validDocument, renderedImage))
                .hasMessage("database is down");

        ArgumentCaptor<String> storedKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(photoStorage).store(storedKeyCaptor.capture(), any(InputStream.class));
        verify(photoStorage).delete(storedKeyCaptor.getValue());
        verify(photoStorage, never()).delete("1b4e28ba-2fa1-11d2-883f-0016d3cca427.png");
    }

    @Test
    void failedStorageLeavesDatabaseUntouched() {
        TreePhoto treePhoto = TreePhoto.createForTree(mock(Tree.class), "a.jpg", "0f8fad5b-d9cb-469f-a165-70867728950e.jpg",
                "image/jpeg", 10, Instant.now());
        when(findTreePhotoHandler.handle(anyString(), any(), any(), any())).thenReturn(treePhoto);
        doThrow(new RuntimeException("disk full")).when(photoStorage).store(anyString(), any(InputStream.class));

        assertThatThrownBy(() -> saveTreePhotoAnnotationHandler.handle("owner@example.com", 1L, 2L, 3L, validDocument, renderedImage))
                .hasMessage("disk full");
        verify(treePhotoAnnotationRepository, never()).save(any());
    }
}

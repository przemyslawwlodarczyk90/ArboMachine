package com.example.arbomachine.domain.referencelayer.service.referencelayerfacade;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.referencelayer.dto.ReferenceLayerResponse;
import com.example.arbomachine.domain.referencelayer.dto.ReferenceLayerUpload;
import com.example.arbomachine.domain.referencelayer.entity.ProjectReferenceLayer;
import com.example.arbomachine.domain.referencelayer.exception.ReferenceLayerFileRejectedException;
import com.example.arbomachine.infrastructure.repository.ProjectReferenceLayerRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Nowa warstwa referencyjna z pliku w WLASNYM projekcie: przeliczenie do GeoJSON WGS 84 i zapis. Nazwa - podana albo
 * nazwa pliku bez rozszerzenia. Limit V1: MAXIMUM_LAYERS_PER_PROJECT warstw w projekcie.
 */
@Component
public class CreateReferenceLayerHandler {
    public static final int MAXIMUM_LAYERS_PER_PROJECT = 20;

    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final ProjectReferenceLayerRepository projectReferenceLayerRepository;
    private final ReferenceLayerFileConverter referenceLayerFileConverter;
    private final ReferenceLayerMapper referenceLayerMapper;

    public CreateReferenceLayerHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                                       ProjectReferenceLayerRepository projectReferenceLayerRepository,
                                       ReferenceLayerFileConverter referenceLayerFileConverter, ReferenceLayerMapper referenceLayerMapper) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.projectReferenceLayerRepository = projectReferenceLayerRepository;
        this.referenceLayerFileConverter = referenceLayerFileConverter;
        this.referenceLayerMapper = referenceLayerMapper;
    }

    @Transactional
    public ReferenceLayerResponse handle(String currentUserEmail, Long projectId, ReferenceLayerUpload upload) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        if (projectReferenceLayerRepository.countByProjectId(ownedProject.getId()) >= MAXIMUM_LAYERS_PER_PROJECT) {
            throw new ReferenceLayerFileRejectedException("TOO_MANY_LAYERS", String.valueOf(MAXIMUM_LAYERS_PER_PROJECT));
        }
        String fileName = upload.originalFileName() == null ? "" : upload.originalFileName().trim();
        ReferenceLayerFileConverter.ConvertedReferenceLayer converted = referenceLayerFileConverter.convert(fileName, upload.content());
        ProjectReferenceLayer layer = ProjectReferenceLayer.create(ownedProject, nameOf(upload.requestedName(), fileName),
                truncate(fileName), converted.format(), converted.sourceCrs(), converted.featureCount(), converted.geoJson(),
                Instant.now().truncatedTo(ChronoUnit.MICROS));
        return referenceLayerMapper.toResponse(projectReferenceLayerRepository.save(layer));
    }

    private static String nameOf(String requestedName, String fileName) {
        if (requestedName != null && !requestedName.isBlank()) return truncate(requestedName.trim());
        String baseName = fileName.replaceAll("^.*[\\\\/]", "").replaceAll("\\.[^.]*$", "");
        return baseName.isBlank() ? "Warstwa referencyjna" : truncate(baseName);
    }

    private static String truncate(String text) {
        return text.length() <= ProjectReferenceLayer.MAXIMUM_NAME_LENGTH ? text : text.substring(0, ProjectReferenceLayer.MAXIMUM_NAME_LENGTH);
    }
}

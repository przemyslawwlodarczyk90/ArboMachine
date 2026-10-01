package com.example.arbomachine.domain.treephoto.service.treephotoannotationfacade;

import com.example.arbomachine.domain.treephoto.dto.PhotoAnnotationDocumentRequest;
import com.example.arbomachine.domain.treephoto.dto.PhotoAnnotationResponse;
import com.example.arbomachine.domain.treephoto.entity.TreePhotoAnnotation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Dokument <-> JSON w bazie i encja -> odpowiedz API (bez klucza storage). Pomocnik handlerow tej fasady. */
final class TreePhotoAnnotationMapper {

    private TreePhotoAnnotationMapper() {
    }

    static String toDocumentJson(ObjectMapper objectMapper, PhotoAnnotationDocumentRequest validatedDocument) {
        try {
            return objectMapper.writeValueAsString(validatedDocument);
        } catch (JsonProcessingException serializationError) {
            throw new IllegalStateException("Could not serialize annotation document", serializationError);
        }
    }

    static PhotoAnnotationResponse toResponse(ObjectMapper objectMapper, TreePhotoAnnotation annotation) {
        try {
            PhotoAnnotationDocumentRequest document = objectMapper.readValue(annotation.getDocumentJson(),
                    PhotoAnnotationDocumentRequest.class);
            return new PhotoAnnotationResponse(annotation.getTreePhoto().getId(), annotation.getSchemaVersion(),
                    document.elements(), annotation.getCreatedAt(), annotation.getUpdatedAt());
        } catch (JsonProcessingException deserializationError) {
            throw new IllegalStateException("Stored annotation document is not readable", deserializationError);
        }
    }
}

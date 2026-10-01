package com.example.arbomachine.domain.treephoto.mapper;

import com.example.arbomachine.domain.treephoto.dto.TreePhotoResponse;
import com.example.arbomachine.domain.treephoto.entity.TreePhoto;
import com.example.arbomachine.domain.treephoto.entity.TreePhotoAnnotation;
import org.springframework.stereotype.Component;

/** Encja TreePhoto (+ jej adnotacja albo null) -> TreePhotoResponse (bez kluczy storage). Z drzewa bierze tylko id. */
@Component
public class TreePhotoMapper {

    public TreePhotoResponse toTreePhotoResponse(TreePhoto treePhoto, TreePhotoAnnotation annotationOrNull) {
        return new TreePhotoResponse(
                treePhoto.getId(),
                treePhoto.getTree().getId(),
                treePhoto.getOriginalFileName(),
                treePhoto.getContentType(),
                treePhoto.getFileSizeBytes(),
                annotationOrNull != null,
                annotationOrNull == null ? null : annotationOrNull.getUpdatedAt(),
                treePhoto.getCreatedAt()
        );
    }
}

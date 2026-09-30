package com.example.arbomachine.domain.treephoto.mapper;

import com.example.arbomachine.domain.treephoto.dto.TreePhotoResponse;
import com.example.arbomachine.domain.treephoto.entity.TreePhoto;
import org.springframework.stereotype.Component;

/** Encja TreePhoto -> TreePhotoResponse (bez klucza storage). Z drzewa bierze tylko id. */
@Component
public class TreePhotoMapper {

    public TreePhotoResponse toTreePhotoResponse(TreePhoto treePhoto) {
        return new TreePhotoResponse(
                treePhoto.getId(),
                treePhoto.getTree().getId(),
                treePhoto.getOriginalFileName(),
                treePhoto.getContentType(),
                treePhoto.getFileSizeBytes(),
                treePhoto.getCreatedAt()
        );
    }
}

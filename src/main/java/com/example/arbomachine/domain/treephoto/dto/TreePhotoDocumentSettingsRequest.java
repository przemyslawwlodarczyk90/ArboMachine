package com.example.arbomachine.domain.treephoto.dto;

import com.example.arbomachine.domain.treephoto.entity.TreePhoto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Etap 52: body PUT .../photos/{photoId}/document-settings - komplet ustawien zdjecia w dokumencie PDF.
 * caption pusty / pominiety = podpis z nazwy pliku.
 */
public record TreePhotoDocumentSettingsRequest(
        @Size(max = TreePhoto.MAXIMUM_CAPTION_LENGTH, message = "Photo caption must have at most 500 characters")
        String caption,

        @NotNull(message = "Choose whether the photo is included in the document")
        Boolean includedInDocument
) {}

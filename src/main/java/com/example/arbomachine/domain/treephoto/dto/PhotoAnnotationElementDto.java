package com.example.arbomachine.domain.treephoto.dto;

import java.util.List;

/**
 * Element dokumentu adnotacji (w requescie i odpowiedzi). type/color/strokeSize to nazwy techniczne
 * (PhotoAnnotationElementType, PhotoAnnotationColor, PhotoAnnotationStrokeSize); text tylko dla TEXT.
 */
public record PhotoAnnotationElementDto(
        String id,
        String type,
        List<NormalizedPointDto> points,
        String color,
        String strokeSize,
        String text
) {}

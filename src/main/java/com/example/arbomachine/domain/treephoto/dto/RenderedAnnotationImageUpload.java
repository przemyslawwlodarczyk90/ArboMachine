package com.example.arbomachine.domain.treephoto.dto;

import org.springframework.core.io.InputStreamSource;

/** Splaszczony PNG z przegladarki (czesc "renderedImage") - wartosci niezaufane, weryfikowane w handlerze. */
public record RenderedAnnotationImageUpload(
        String declaredContentType,
        long fileSizeBytes,
        InputStreamSource content
) {}

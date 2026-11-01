package com.example.arbomachine.domain.projectdocument.generator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Ogranicza liczbe jednoczesnych generowan dokumentu PDF (domyslnie 1). Dokument ze zdjeciami zajmuje setki MB sterty
 * (zdjecia dekodowane w pelnej rozdzielczosci) - dwa naraz z duzym zdjeciem konczyly sie OutOfMemoryError na malym
 * serwerze (AWS_RESOURCE_AUDIT.md). Kolejne zadanie czeka w kolejce do max-wait-seconds, potem dostaje 503.
 */
@Component
public class DocumentGenerationLimiter {

    private final Semaphore generationPermits;
    private final long maximumWaitSeconds;

    public DocumentGenerationLimiter(@Value("${app.project-documents.max-concurrent-generations:1}") int maximumConcurrentGenerations,
                                     @Value("${app.project-documents.max-wait-seconds:120}") long maximumWaitSeconds) {
        if (maximumConcurrentGenerations < 1) throw new IllegalArgumentException("At least one concurrent generation is required");
        this.generationPermits = new Semaphore(maximumConcurrentGenerations, true);
        this.maximumWaitSeconds = maximumWaitSeconds;
    }

    public <T> T runLimited(Supplier<T> generation) {
        boolean permitAcquired;
        try {
            permitAcquired = generationPermits.tryAcquire(maximumWaitSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException interruption) {
            Thread.currentThread().interrupt();
            throw new DocumentGenerationBusyException("Interrupted while waiting for document generation");
        }
        if (!permitAcquired) throw new DocumentGenerationBusyException("Document generation is busy");
        try {
            return generation.get();
        } finally {
            generationPermits.release();
        }
    }
}

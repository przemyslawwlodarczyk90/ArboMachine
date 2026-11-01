package com.example.arbomachine.domain.projectdocument.generator;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentGenerationLimiterTest {

    @Test
    void shouldRejectSecondGenerationWhenOnlyOnePermitAndWaitTimeIsOver() throws Exception {
        DocumentGenerationLimiter limiter = new DocumentGenerationLimiter(1, 0);
        CountDownLatch firstGenerationStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstGeneration = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<String> firstGeneration = executor.submit(() -> limiter.runLimited(() -> {
                firstGenerationStarted.countDown();
                awaitQuietly(releaseFirstGeneration);
                return "first";
            }));
            assertThat(firstGenerationStarted.await(5, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> limiter.runLimited(() -> "second")).isInstanceOf(DocumentGenerationBusyException.class);

            releaseFirstGeneration.countDown();
            assertThat(firstGeneration.get(5, TimeUnit.SECONDS)).isEqualTo("first");
            assertThat(limiter.runLimited(() -> "third")).isEqualTo("third");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldReleasePermitWhenGenerationFails() {
        DocumentGenerationLimiter limiter = new DocumentGenerationLimiter(1, 0);
        assertThatThrownBy(() -> limiter.runLimited(() -> { throw new IllegalStateException("render error"); }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(limiter.runLimited(() -> "next")).isEqualTo("next");
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException interruption) {
            Thread.currentThread().interrupt();
        }
    }
}

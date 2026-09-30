package com.example.arbomachine.infrastructure.storage;

import com.example.arbomachine.domain.treephoto.config.TreePhotoProperties;
import com.example.arbomachine.domain.treephoto.storage.PhotoStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.util.unit.DataSize;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Storage zdjec na dysku: zapis/odczyt/usuniecie w katalogu tymczasowym i odporność na zle klucze. */
class FileSystemPhotoStorageTest {

    private static final String VALID_STORAGE_KEY = "0f8fad5b-d9cb-469f-a165-70867728950e.jpg";

    @TempDir
    Path temporaryDirectory;

    private Path storageRootDirectory;
    private FileSystemPhotoStorage fileSystemPhotoStorage;

    @BeforeEach
    void createStorage() {
        storageRootDirectory = temporaryDirectory.resolve("tree-photos");
        fileSystemPhotoStorage = new FileSystemPhotoStorage(
                new TreePhotoProperties(DataSize.ofMegabytes(1), storageRootDirectory));
    }

    @Test
    void storesLoadsAndDeletesFileUnderKey() throws Exception {
        fileSystemPhotoStorage.store(VALID_STORAGE_KEY, new ByteArrayInputStream(new byte[]{1, 2, 3}));
        assertThat(Files.readAllBytes(storageRootDirectory.resolve(VALID_STORAGE_KEY))).containsExactly(1, 2, 3);
        assertThat(fileSystemPhotoStorage.load(VALID_STORAGE_KEY).getContentAsByteArray()).containsExactly(1, 2, 3);

        fileSystemPhotoStorage.delete(VALID_STORAGE_KEY);
        assertThat(storageRootDirectory.resolve(VALID_STORAGE_KEY)).doesNotExist();
        // Ponowne usuniecie nieistniejacego pliku nie jest bledem.
        fileSystemPhotoStorage.delete(VALID_STORAGE_KEY);
    }

    @Test
    void neverOverwritesExistingFile() throws Exception {
        fileSystemPhotoStorage.store(VALID_STORAGE_KEY, new ByteArrayInputStream(new byte[]{1}));
        assertThatThrownBy(() -> fileSystemPhotoStorage.store(VALID_STORAGE_KEY, new ByteArrayInputStream(new byte[]{9})))
                .isInstanceOf(PhotoStorageException.class);
        assertThat(Files.readAllBytes(storageRootDirectory.resolve(VALID_STORAGE_KEY))).containsExactly(1);
    }

    @Test
    void missingFileCannotBeLoaded() {
        assertThatThrownBy(() -> fileSystemPhotoStorage.load(VALID_STORAGE_KEY)).isInstanceOf(PhotoStorageException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"../evil.jpg", "..\\evil.jpg", "/etc/passwd", "C:\\evil.jpg", "evil.jpg",
            "0f8fad5b-d9cb-469f-a165-70867728950e.exe", "sub/0f8fad5b-d9cb-469f-a165-70867728950e.jpg", ""})
    void rejectsKeysOutsideTheGeneratedFormat(String dangerousStorageKey) {
        assertThatThrownBy(() -> fileSystemPhotoStorage.store(dangerousStorageKey, new ByteArrayInputStream(new byte[]{1})))
                .isInstanceOf(PhotoStorageException.class);
        assertThatThrownBy(() -> fileSystemPhotoStorage.load(dangerousStorageKey)).isInstanceOf(PhotoStorageException.class);
        assertThatThrownBy(() -> fileSystemPhotoStorage.delete(dangerousStorageKey)).isInstanceOf(PhotoStorageException.class);
        assertThat(temporaryDirectory.resolve("evil.jpg")).doesNotExist();
    }
}

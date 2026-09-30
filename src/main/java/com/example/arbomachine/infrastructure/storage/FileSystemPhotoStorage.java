package com.example.arbomachine.infrastructure.storage;

import com.example.arbomachine.domain.treephoto.config.TreePhotoProperties;
import com.example.arbomachine.domain.treephoto.storage.PhotoStorage;
import com.example.arbomachine.domain.treephoto.storage.PhotoStorageException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Storage zdjec V1: pliki w katalogu app.tree-photos.storage-directory, metadane w bazie (TreePhoto).
 * Klucz to UUID z rozszerzeniem nadany przez domene - akceptujemy wylacznie taki format, a sciezke dodatkowo
 * sprawdzamy po normalizacji, wiec "../", sciezki absolutne i separatory nie maja szans wyjsc poza katalog.
 * Zapis nigdy nie nadpisuje istniejacego pliku.
 */
@Component
public class FileSystemPhotoStorage implements PhotoStorage {

    /** Dokladnie format kluczy z UploadTreePhotoHandler: UUID + rozszerzenie dozwolonego typu zdjecia. */
    private static final Pattern STORAGE_KEY_PATTERN =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)");

    private final Path storageRootDirectory;

    public FileSystemPhotoStorage(TreePhotoProperties treePhotoProperties) {
        this.storageRootDirectory = treePhotoProperties.storageDirectory().toAbsolutePath().normalize();
    }

    @Override
    public void store(String storageKey, InputStream photoContent) {
        Path targetFile = resolveInsideStorageRoot(storageKey);
        try {
            Files.createDirectories(storageRootDirectory);
            // Bez REPLACE_EXISTING: zajety klucz konczy sie bledem zamiast nadpisania cudzego pliku.
            Files.copy(photoContent, targetFile);
        } catch (IOException storingError) {
            throw new PhotoStorageException("Could not store photo file", storingError);
        }
    }

    @Override
    public Resource load(String storageKey) {
        Path photoFile = resolveInsideStorageRoot(storageKey);
        if (!Files.isRegularFile(photoFile)) {
            throw new PhotoStorageException("Photo file does not exist in storage");
        }
        return new FileSystemResource(photoFile);
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolveInsideStorageRoot(storageKey));
        } catch (IOException deletingError) {
            throw new PhotoStorageException("Could not delete photo file", deletingError);
        }
    }

    private Path resolveInsideStorageRoot(String storageKey) {
        if (storageKey == null || !STORAGE_KEY_PATTERN.matcher(storageKey).matches()) {
            throw new PhotoStorageException("Invalid photo storage key");
        }
        Path resolvedFile = storageRootDirectory.resolve(storageKey).normalize();
        if (!resolvedFile.getParent().equals(storageRootDirectory)) {
            throw new PhotoStorageException("Invalid photo storage key");
        }
        return resolvedFile;
    }
}

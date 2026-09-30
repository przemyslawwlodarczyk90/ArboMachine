package com.example.arbomachine.domain.treephoto.service.treephotofacade;

import java.util.Arrays;
import java.util.Optional;

/**
 * Obslugiwane formaty zdjec V1 i ich sygnatury ("magic bytes"). Typ pliku ustalamy z zawartosci, a nie z nazwy -
 * dowolny plik nazwany "cos.jpg" nie przejdzie. WebP jest obslugiwany, bo nic nie kosztuje (przegladarki wyswietlaja
 * go natywnie); HEIC z iPhone'a - nie (Safari przy wyborze z galerii zwykle i tak wysyla JPEG), patrz
 * docs/DO_DOPRECYZOWANIA.md.
 */
enum SupportedTreePhotoType {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp");

    /** Tyle bajtow z poczatku pliku wystarcza do rozpoznania kazdego z formatow. */
    static final int SIGNATURE_LENGTH = 12;

    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] RIFF_SIGNATURE = {'R', 'I', 'F', 'F'};
    private static final byte[] WEBP_SIGNATURE = {'W', 'E', 'B', 'P'};

    private final String contentType;
    private final String fileExtension;

    SupportedTreePhotoType(String contentType, String fileExtension) {
        this.contentType = contentType;
        this.fileExtension = fileExtension;
    }

    String contentType() {
        return contentType;
    }

    String fileExtension() {
        return fileExtension;
    }

    static Optional<SupportedTreePhotoType> ofDeclaredContentType(String declaredContentType) {
        return Arrays.stream(values())
                .filter(supportedType -> supportedType.contentType.equalsIgnoreCase(
                        declaredContentType == null ? "" : declaredContentType.trim()))
                .findFirst();
    }

    /** Czy poczatek pliku (pierwsze SIGNATURE_LENGTH bajtow albo mniej) ma sygnature tego formatu. */
    boolean matchesSignatureOf(byte[] fileStart) {
        return switch (this) {
            case JPEG -> startsWith(fileStart, JPEG_SIGNATURE, 0);
            case PNG -> startsWith(fileStart, PNG_SIGNATURE, 0);
            // RIFF <4 bajty rozmiaru> WEBP
            case WEBP -> startsWith(fileStart, RIFF_SIGNATURE, 0) && startsWith(fileStart, WEBP_SIGNATURE, 8);
        };
    }

    private static boolean startsWith(byte[] fileStart, byte[] signature, int offset) {
        if (fileStart.length < offset + signature.length) return false;
        for (int index = 0; index < signature.length; index++) {
            if (fileStart[offset + index] != signature[index]) return false;
        }
        return true;
    }
}

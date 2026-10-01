package com.example.arbomachine.domain.projectdocument.data;

import java.io.IOException;
import java.io.InputStream;

/**
 * Leniwy dostep do tresci obrazu - plik jest otwierany dopiero, gdy generator go potrzebuje, wiec zdjecia
 * nie leza wszystkie naraz w pamieci. Generator nie zna PhotoStorage ani kluczy plikow.
 */
@FunctionalInterface
public interface DocumentImageContent {
    InputStream openStream() throws IOException;
}

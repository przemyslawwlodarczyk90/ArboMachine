// Szybka kontrola wybranego pliku w przegladarce - tylko UX. Zrodlem prawdy jest backend, ktory sprawdza typ
// z zawartosci pliku i limit rozmiaru (app.tree-photos.max-file-size). Modul bez importow - testowany przez node --test.

/** Te same typy, ktore przyjmuje backend (SupportedTreePhotoType). */
export const SUPPORTED_TREE_PHOTO_CONTENT_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

/** Komunikat bledu dla wybranego pliku albo null, gdy plik mozna wyslac. */
export const errorOfSelectedPhotoFile = (selectedFile: { type: string; size: number }): string | null => {
  if (selectedFile.size === 0) return 'Wybrany plik jest pusty.';
  if (!SUPPORTED_TREE_PHOTO_CONTENT_TYPES.includes(selectedFile.type)) {
    return 'Nieobsługiwany format pliku. Wybierz zdjęcie JPEG, PNG albo WebP.';
  }
  return null;
};

/** Rozmiar pliku do wyswietlenia, np. "850 KB", "3,4 MB". */
export const formatFileSizeForDisplay = (fileSizeBytes: number): string => {
  if (fileSizeBytes < 1024) return `${fileSizeBytes} B`;
  if (fileSizeBytes < 1024 * 1024) return `${Math.round(fileSizeBytes / 1024)} KB`;
  return `${(fileSizeBytes / (1024 * 1024)).toFixed(1).replace('.', ',')} MB`;
};

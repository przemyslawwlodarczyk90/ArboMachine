import type { TreePhoto, TreePhotoDocumentSettingsRequest } from './treePhotoDataTypes.ts';

// Etap 52: podpis zdjecia i wybor "w dokumencie PDF". Modul bez Reacta - testowany przez node --test.

export const MAXIMUM_PHOTO_CAPTION_LENGTH = 500;

export interface PhotoDocumentSettingsValues {
  caption: string;
  includedInDocument: boolean;
}

export const toPhotoDocumentSettingsValues = (treePhoto: Pick<TreePhoto, 'caption' | 'includedInDocument'>): PhotoDocumentSettingsValues => ({
  caption: treePhoto.caption ?? '',
  // Zdjecia z odpowiedzi sprzed etapu 52 (np. kopia offline) nie maja pola - sa w dokumencie.
  includedInDocument: treePhoto.includedInDocument ?? true,
});

/** Pusty podpis = brak (dokument uzyje nazwy pliku). */
export const toPhotoDocumentSettingsRequest = (values: PhotoDocumentSettingsValues): TreePhotoDocumentSettingsRequest => ({
  caption: values.caption.trim() || undefined,
  includedInDocument: values.includedInDocument,
});

export const validatePhotoDocumentSettings = (values: PhotoDocumentSettingsValues): string | null =>
  values.caption.trim().length > MAXIMUM_PHOTO_CAPTION_LENGTH
    ? `Podpis może mieć maksymalnie ${MAXIMUM_PHOTO_CAPTION_LENGTH} znaków.`
    : null;

export const arePhotoDocumentSettingsEquivalent = (first: PhotoDocumentSettingsValues, second: PhotoDocumentSettingsValues) =>
  JSON.stringify(toPhotoDocumentSettingsRequest(first)) === JSON.stringify(toPhotoDocumentSettingsRequest(second));

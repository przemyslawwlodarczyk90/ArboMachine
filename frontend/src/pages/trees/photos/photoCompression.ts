// Etap 40: zmniejszanie zdjecia na urzadzeniu przed wyslaniem (albo przed zapisem bez zasiegu) - reguly bez
// przegladarki, testowane przez node --test. Samo przeskalowanie: photoCompressionInBrowser.ts.
import type { KeyValueStorage } from '../../../map/basemapPreference.ts';

/** Dluzszy bok po zmniejszeniu - V1: 2560 px wystarcza do dokumentacji (A4 w 300 dpi to ok. 3500 px). */
export const COMPRESSED_PHOTO_MAX_LONG_SIDE_PX = 2560;
export const COMPRESSED_PHOTO_JPEG_QUALITY = 0.85;
/** Mniejszych plikow nie ruszamy - oszczednosc niewielka, a strata danych EXIF ta sama. */
export const MIN_PHOTO_SIZE_FOR_COMPRESSION_BYTES = 1024 * 1024;

/**
 * Czy zdjecie mozna zmniejszyc: tylko JPEG i WebP (zdjecia z aparatu). PNG zostaje bez zmian - bywa zrzutem ekranu
 * albo rysunkiem, gdzie JPEG psuje ostre krawedzie i przezroczystosc.
 */
export const canCompressPhoto = (file: { type: string; size: number }) =>
  (file.type === 'image/jpeg' || file.type === 'image/webp') && file.size > MIN_PHOTO_SIZE_FOR_COMPRESSION_BYTES;

/** Wymiary po zmniejszeniu z zachowaniem proporcji; mniejsze zdjecie zostaje w swoich wymiarach (tylko ponowny zapis JPEG). */
export function scaledPhotoDimensions(width: number, height: number, maxLongSide = COMPRESSED_PHOTO_MAX_LONG_SIDE_PX) {
  const longSide = Math.max(width, height);
  if (longSide <= maxLongSide) return { width, height };
  const scale = maxLongSide / longSide;
  return { width: Math.max(1, Math.round(width * scale)), height: Math.max(1, Math.round(height * scale)) };
}

/** Nazwa pliku po zmianie formatu na JPEG: IMG_1.webp -> IMG_1.jpg (nazwa widoczna na liscie zdjec). */
export function compressedPhotoFileName(originalName: string): string {
  const dotIndex = originalName.lastIndexOf('.');
  const baseName = dotIndex > 0 ? originalName.slice(0, dotIndex) : originalName;
  return `${baseName || 'zdjecie'}.jpg`;
}

/** Zmniejszona wersja ma sens tylko, gdy jest wyraznie mniejsza (co najmniej 10%) - inaczej wysylamy oryginal. */
export const isCompressedPhotoWorthUsing = (originalSizeBytes: number, compressedSizeBytes: number) =>
  compressedSizeBytes > 0 && compressedSizeBytes < originalSizeBytes * 0.9;

const COMPRESS_PHOTOS_KEY = 'arbomachine.photos.compressBeforeUpload';

/**
 * Wybor zapamietany na urzadzeniu; domyslnie wylaczony - zmniejszone zdjecie traci dane EXIF (date, polozenie
 * z aparatu) i rozdzielczosc, a nie wiemy jeszcze, czy klient ich potrzebuje (02 poz. 4.1).
 */
export function readCompressPhotosPreference(storage: KeyValueStorage | null): boolean {
  try {
    return storage?.getItem(COMPRESS_PHOTOS_KEY) === 'true';
  } catch {
    return false;
  }
}

export function saveCompressPhotosPreference(storage: KeyValueStorage | null, compressPhotos: boolean) {
  try {
    storage?.setItem(COMPRESS_PHOTOS_KEY, String(compressPhotos));
  } catch {
    // Zablokowany magazyn - wybor obowiazuje do zamkniecia okna.
  }
}

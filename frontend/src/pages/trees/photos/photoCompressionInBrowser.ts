import {
  canCompressPhoto, COMPRESSED_PHOTO_JPEG_QUALITY, compressedPhotoFileName, isCompressedPhotoWorthUsing, scaledPhotoDimensions,
} from './photoCompression';

export interface PhotoCompressionResult {
  file: File;
  /** false = wysylamy oryginal (format bez zmniejszania, maly plik, brak oszczednosci albo przegladarka nie umie) */
  wasCompressed: boolean;
}

/**
 * Etap 40: zmniejszenie zdjecia w przegladarce (canvas -> JPEG). Orientacja z EXIF jest uwzgledniana przy odczycie
 * (imageOrientation: 'from-image'), pozostale dane EXIF przepadaja. Kazdy blad = oryginal - zmniejszanie nigdy nie
 * blokuje dodania zdjecia.
 */
export async function compressPhotoInBrowser(file: File): Promise<PhotoCompressionResult> {
  if (!canCompressPhoto(file) || typeof createImageBitmap !== 'function') return { file, wasCompressed: false };
  try {
    const bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' });
    const { width, height } = scaledPhotoDimensions(bitmap.width, bitmap.height);
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const context = canvas.getContext('2d');
    if (!context) {
      bitmap.close();
      return { file, wasCompressed: false };
    }
    context.drawImage(bitmap, 0, 0, width, height);
    bitmap.close();
    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/jpeg', COMPRESSED_PHOTO_JPEG_QUALITY));
    if (!blob || !isCompressedPhotoWorthUsing(file.size, blob.size)) return { file, wasCompressed: false };
    return {
      file: new File([blob], compressedPhotoFileName(file.name), { type: 'image/jpeg', lastModified: file.lastModified }),
      wasCompressed: true,
    };
  } catch {
    return { file, wasCompressed: false };
  }
}

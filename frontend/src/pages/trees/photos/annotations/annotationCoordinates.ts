import type { NormalizedPoint } from './annotationDataTypes';

// Przeliczenia piksele <-> wspolrzedne znormalizowane (0..1). Rysunek zapisujemy wzgledem zdjecia, a nie ekranu,
// wiec pozostaje poprawny na tablecie, desktopie, miniaturze i przy renderowaniu w pelnej rozdzielczosci.

/** 6 miejsc po przecinku - jak w backendzie; o rzedy wielkosci dokladniej niz piksel nawet duzego zdjecia. */
const NORMALIZED_DECIMAL_PLACES = 6;

const clampToUnitRange = (value: number) => Math.min(1, Math.max(0, value));
const roundNormalized = (value: number) => Number(value.toFixed(NORMALIZED_DECIMAL_PLACES));

/** Punkt wskaznika (np. clientX/clientY) wzgledem prostokata obrazu -> punkt znormalizowany, przyciety do 0..1. */
export function toNormalizedPoint(pixelX: number, pixelY: number,
                                  imageRect: { left: number; top: number; width: number; height: number }): NormalizedPoint {
  return {
    x: roundNormalized(clampToUnitRange((pixelX - imageRect.left) / imageRect.width)),
    y: roundNormalized(clampToUnitRange((pixelY - imageRect.top) / imageRect.height)),
  };
}

/** Punkt znormalizowany -> piksele obrazu o podanych wymiarach. */
export function toPixelPoint(point: NormalizedPoint, width: number, height: number): { x: number; y: number } {
  return { x: point.x * width, y: point.y * height };
}

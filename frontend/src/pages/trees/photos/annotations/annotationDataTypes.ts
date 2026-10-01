// Typy i katalog techniczny adnotacji zdjec - odpowiadaja backendowi (domain.treephoto.annotation, dto).
// Modul bez importow wartosci - testowany przez node --test.

export type AnnotationElementType = 'LINE' | 'ARROW' | 'FREEHAND' | 'TEXT';
export type AnnotationColor = 'RED' | 'YELLOW' | 'BLUE' | 'BLACK' | 'WHITE';
export type AnnotationStrokeSize = 'SMALL' | 'MEDIUM' | 'LARGE';

/** Punkt we wspolrzednych znormalizowanych do zdjecia: 0..1 (nie piksele ekranu). */
export interface NormalizedPoint {
  x: number;
  y: number;
}

export interface AnnotationElement {
  id: string;
  type: AnnotationElementType;
  points: NormalizedPoint[];
  color: AnnotationColor;
  strokeSize: AnnotationStrokeSize;
  text?: string | null;
}

/** Dokument wysylany do PUT .../annotation (czesc "document"). */
export interface AnnotationDocument {
  schemaVersion: 1;
  elements: AnnotationElement[];
}

/** PhotoAnnotationResponse z backendu. */
export interface PhotoAnnotation {
  photoId: number;
  schemaVersion: number;
  elements: AnnotationElement[];
  createdAt: string;
  updatedAt: string;
}

/** Paleta V1: kolor rysowania + polska nazwa w UI. Kolor nie ma znaczenia arborystycznego. */
export const ANNOTATION_COLORS: { color: AnnotationColor; label: string; hex: string }[] = [
  { color: 'RED', label: 'Czerwony', hex: '#E53935' },
  { color: 'YELLOW', label: 'Żółty', hex: '#FDD835' },
  { color: 'BLUE', label: 'Niebieski', hex: '#1E88E5' },
  { color: 'BLACK', label: 'Czarny', hex: '#000000' },
  { color: 'WHITE', label: 'Biały', hex: '#FFFFFF' },
];

/**
 * Grubosci V1 - wzgledne do krotszego boku zdjecia, wiec rysunek wyglada tak samo na tablecie, miniaturze
 * i w pelnej rozdzielczosci. fontRatio - wielkosc tekstu dla narzedzia "Tekst".
 */
export const ANNOTATION_STROKE_SIZES: { strokeSize: AnnotationStrokeSize; label: string; widthRatio: number; fontRatio: number }[] = [
  { strokeSize: 'SMALL', label: 'Cienka', widthRatio: 0.004, fontRatio: 0.035 },
  { strokeSize: 'MEDIUM', label: 'Średnia', widthRatio: 0.008, fontRatio: 0.05 },
  { strokeSize: 'LARGE', label: 'Gruba', widthRatio: 0.014, fontRatio: 0.07 },
];

/** Limity jak w backendzie (PhotoAnnotationLimits). */
export const ANNOTATION_LIMITS = {
  maximumElementCount: 200,
  maximumTextLength: 500,
  maximumFreehandPointCount: 1000,
};

export const colorHexOf = (color: AnnotationColor) =>
  ANNOTATION_COLORS.find((paletteColor) => paletteColor.color === color)?.hex ?? '#E53935';

export const strokeSizeOf = (strokeSize: AnnotationStrokeSize) =>
  ANNOTATION_STROKE_SIZES.find((size) => size.strokeSize === strokeSize) ?? ANNOTATION_STROKE_SIZES[1];

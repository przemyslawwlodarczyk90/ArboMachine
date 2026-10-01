import {
  ANNOTATION_LIMITS, type AnnotationColor, type AnnotationDocument, type AnnotationElement, type AnnotationElementType,
  type AnnotationStrokeSize, type NormalizedPoint,
} from './annotationDataTypes.ts';

// Budowanie elementow adnotacji z gestow i dokumentu do API. Modul testowany przez node --test.

/** Minimalny ruch (we wspolrzednych 0..1), zeby dopisac punkt do linii odrecznej - ogranicza liczbe punktow. */
const MINIMUM_FREEHAND_STEP = 0.002;
/** Linia/strzalka krotsza niz to (przypadkowe dotkniecie) nie jest zapisywana. */
const MINIMUM_SEGMENT_LENGTH = 0.005;

let elementSequence = 0;
/** Krotki, unikalny w dokumencie identyfikator elementu ([A-Za-z0-9_-], jak wymaga backend). */
export const createAnnotationElementId = () =>
  `el-${Date.now().toString(36)}-${(elementSequence++).toString(36)}-${Math.random().toString(36).slice(2, 6)}`;

const distance = (first: NormalizedPoint, second: NormalizedPoint) => Math.hypot(first.x - second.x, first.y - second.y);

/** Dopisuje punkt do linii odrecznej tylko przy wyraznym ruchu i w limicie punktow. */
export function appendFreehandPoint(points: NormalizedPoint[], point: NormalizedPoint): NormalizedPoint[] {
  const lastPoint = points[points.length - 1];
  if (lastPoint && distance(lastPoint, point) < MINIMUM_FREEHAND_STEP) return points;
  if (points.length >= ANNOTATION_LIMITS.maximumFreehandPointCount) return points;
  return [...points, point];
}

export interface AnnotationToolSettings {
  color: AnnotationColor;
  strokeSize: AnnotationStrokeSize;
}

/**
 * Element gotowy do zapisu albo null, gdy gest jest niepelny (np. klikniecie bez przeciagniecia przy linii,
 * pusty tekst). LINE/ARROW - 2 punkty, TEXT - 1 punkt, FREEHAND - od 2 punktow.
 */
export function createAnnotationElement(type: AnnotationElementType, points: NormalizedPoint[],
                                        settings: AnnotationToolSettings, text = '',
                                        createId: () => string = createAnnotationElementId): AnnotationElement | null {
  const base = { id: createId(), type, color: settings.color, strokeSize: settings.strokeSize };
  switch (type) {
    case 'LINE':
    case 'ARROW': {
      if (points.length < 2) return null;
      const segment = [points[0], points[points.length - 1]];
      return distance(segment[0], segment[1]) < MINIMUM_SEGMENT_LENGTH ? null : { ...base, points: segment };
    }
    case 'FREEHAND':
      return points.length < 2 ? null : { ...base, points: points.slice(0, ANNOTATION_LIMITS.maximumFreehandPointCount) };
    case 'TEXT': {
      const trimmedText = text.trim().slice(0, ANNOTATION_LIMITS.maximumTextLength);
      return points.length < 1 || !trimmedText ? null : { ...base, points: [points[0]], text: trimmedText };
    }
  }
}

/** Dokument dla backendu (schemaVersion 1); tekst tylko przy elementach TEXT. */
export function toAnnotationDocument(elements: AnnotationElement[]): AnnotationDocument {
  return {
    schemaVersion: 1,
    elements: elements.map((element) => ({
      id: element.id,
      type: element.type,
      points: element.points.map((point) => ({ x: point.x, y: point.y })),
      color: element.color,
      strokeSize: element.strokeSize,
      ...(element.type === 'TEXT' ? { text: element.text ?? '' } : {}),
    })),
  };
}

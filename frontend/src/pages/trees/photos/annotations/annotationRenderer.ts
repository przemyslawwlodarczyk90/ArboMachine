import { colorHexOf, strokeSizeOf, type AnnotationElement } from './annotationDataTypes.ts';
import { toPixelPoint } from './annotationCoordinates.ts';

// Rysowanie dokumentu adnotacji na kontekscie Canvas 2D o dowolnych wymiarach - ten sam kod rysuje podglad
// w edytorze i splaszczony PNG w rozdzielczosci zdjecia. Kontekst jest minimalnym interfejsem, wiec w testach
// mozna podstawic obiekt nagrywajacy wywolania zamiast prawdziwego canvasu.

export interface AnnotationDrawingContext {
  strokeStyle: string | CanvasGradient | CanvasPattern;
  fillStyle: string | CanvasGradient | CanvasPattern;
  lineWidth: number;
  lineCap: CanvasLineCap;
  lineJoin: CanvasLineJoin;
  font: string;
  textBaseline: CanvasTextBaseline;
  save(): void;
  restore(): void;
  beginPath(): void;
  moveTo(x: number, y: number): void;
  lineTo(x: number, y: number): void;
  closePath(): void;
  stroke(): void;
  fill(): void;
  strokeText(text: string, x: number, y: number): void;
  fillText(text: string, x: number, y: number): void;
}

/** Jasne kolory dostaja ciemna obwodke tekstu, ciemne - jasna (czytelnosc na kazdym zdjeciu). */
const LIGHT_COLORS = new Set(['YELLOW', 'WHITE']);

export function drawAnnotationElements(context: AnnotationDrawingContext, elements: AnnotationElement[],
                                       width: number, height: number): void {
  const shorterSide = Math.min(width, height);
  for (const element of elements) {
    const size = strokeSizeOf(element.strokeSize);
    const lineWidth = Math.max(1, size.widthRatio * shorterSide);
    const color = colorHexOf(element.color);
    const pixelPoints = element.points.map((point) => toPixelPoint(point, width, height));
    context.save();
    context.strokeStyle = color;
    context.fillStyle = color;
    context.lineWidth = lineWidth;
    context.lineCap = 'round';
    context.lineJoin = 'round';

    if (element.type === 'TEXT' && element.text) {
      const fontSize = Math.max(10, size.fontRatio * shorterSide);
      context.font = `bold ${fontSize}px system-ui, sans-serif`;
      context.textBaseline = 'top';
      context.lineWidth = Math.max(2, fontSize * 0.15);
      context.strokeStyle = LIGHT_COLORS.has(element.color) ? '#000000' : '#FFFFFF';
      context.strokeText(element.text, pixelPoints[0].x, pixelPoints[0].y);
      context.fillText(element.text, pixelPoints[0].x, pixelPoints[0].y);
    } else if (pixelPoints.length >= 2) {
      context.beginPath();
      context.moveTo(pixelPoints[0].x, pixelPoints[0].y);
      for (const point of pixelPoints.slice(1)) context.lineTo(point.x, point.y);
      context.stroke();
      if (element.type === 'ARROW') drawArrowHead(context, pixelPoints[0], pixelPoints[1], lineWidth, shorterSide);
    }
    context.restore();
  }
}

/** Grot strzalki na koncu odcinka - trojkat, dlugosc zalezna od grubosci linii i rozmiaru zdjecia. */
function drawArrowHead(context: AnnotationDrawingContext, start: { x: number; y: number }, end: { x: number; y: number },
                       lineWidth: number, shorterSide: number) {
  const headLength = Math.max(lineWidth * 4, shorterSide * 0.03);
  const angle = Math.atan2(end.y - start.y, end.x - start.x);
  context.beginPath();
  context.moveTo(end.x, end.y);
  context.lineTo(end.x - headLength * Math.cos(angle - Math.PI / 7), end.y - headLength * Math.sin(angle - Math.PI / 7));
  context.lineTo(end.x - headLength * Math.cos(angle + Math.PI / 7), end.y - headLength * Math.sin(angle + Math.PI / 7));
  context.closePath();
  context.fill();
}

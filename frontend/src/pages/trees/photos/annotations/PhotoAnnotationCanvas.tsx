import { useEffect, useRef, useState, type PointerEvent } from 'react';
import { toNormalizedPoint } from './annotationCoordinates';
import type { AnnotationElement, AnnotationElementType, NormalizedPoint } from './annotationDataTypes';
import { appendFreehandPoint, createAnnotationElement, type AnnotationToolSettings } from './annotationElements';
import { drawAnnotationElements } from './annotationRenderer';

/**
 * Plotno edytora adnotacji (Canvas 2D): zdjecie + zapisane elementy + rysowany wlasnie element. Obsluguje mysz,
 * dotyk i rysik jednym kodem (Pointer Events); przewijanie strony gestem jest na plotnie wylaczone (touch-action).
 * Wymiary: szerokosc kontenera, proporcje zdjecia, wysokosc ograniczona do ekranu; ostre na ekranach HiDPI.
 * Nie zna API - gotowy element oddaje przez onElementCompleted.
 */
export function PhotoAnnotationCanvas({ image, elements, tool, settings, pendingText, onElementCompleted, onTextToolWithoutText }: {
  image: HTMLImageElement;
  elements: AnnotationElement[];
  tool: AnnotationElementType;
  settings: AnnotationToolSettings;
  pendingText: string;
  onElementCompleted: (element: AnnotationElement) => void;
  onTextToolWithoutText: () => void;
}) {
  const containerRef = useRef<HTMLDivElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [displaySize, setDisplaySize] = useState({ width: 0, height: 0 });
  const [draftPoints, setDraftPoints] = useState<NormalizedPoint[] | null>(null);

  // Rozmiar wyswietlania: szerokosc kontenera, proporcje zdjecia, maks. ~70% wysokosci okna.
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;
    const updateDisplaySize = () => {
      const aspectRatio = image.naturalHeight / image.naturalWidth;
      const maximumHeight = window.innerHeight * 0.7;
      let width = container.clientWidth;
      let height = width * aspectRatio;
      if (height > maximumHeight) {
        height = maximumHeight;
        width = height / aspectRatio;
      }
      setDisplaySize({ width: Math.floor(width), height: Math.floor(height) });
    };
    updateDisplaySize();
    const resizeObserver = new ResizeObserver(updateDisplaySize);
    resizeObserver.observe(container);
    return () => resizeObserver.disconnect();
  }, [image]);

  const draftElement = draftPoints
    ? createAnnotationElement(tool, draftPoints, settings, pendingText, () => 'draft')
    : null;

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas || displaySize.width === 0) return;
    const pixelRatio = window.devicePixelRatio || 1;
    canvas.width = Math.round(displaySize.width * pixelRatio);
    canvas.height = Math.round(displaySize.height * pixelRatio);
    const context = canvas.getContext('2d');
    if (!context) return;
    context.setTransform(pixelRatio, 0, 0, pixelRatio, 0, 0);
    context.drawImage(image, 0, 0, displaySize.width, displaySize.height);
    drawAnnotationElements(context, draftElement ? [...elements, draftElement] : elements, displaySize.width, displaySize.height);
  }, [image, elements, draftElement, displaySize]);

  const pointOf = (pointerEvent: PointerEvent<HTMLCanvasElement>) =>
    toNormalizedPoint(pointerEvent.clientX, pointerEvent.clientY, pointerEvent.currentTarget.getBoundingClientRect());

  const startDrawing = (pointerEvent: PointerEvent<HTMLCanvasElement>) => {
    if (pointerEvent.button !== 0 && pointerEvent.pointerType === 'mouse') return;
    const point = pointOf(pointerEvent);
    if (tool === 'TEXT') {
      const textElement = createAnnotationElement('TEXT', [point], settings, pendingText);
      if (textElement) onElementCompleted(textElement);
      else onTextToolWithoutText();
      return;
    }
    pointerEvent.currentTarget.setPointerCapture(pointerEvent.pointerId);
    setDraftPoints(tool === 'FREEHAND' ? [point] : [point, point]);
  };

  const continueDrawing = (pointerEvent: PointerEvent<HTMLCanvasElement>) => {
    if (!draftPoints) return;
    const point = pointOf(pointerEvent);
    setDraftPoints((currentPoints) => {
      if (!currentPoints) return currentPoints;
      return tool === 'FREEHAND' ? appendFreehandPoint(currentPoints, point) : [currentPoints[0], point];
    });
  };

  const finishDrawing = () => {
    if (!draftPoints) return;
    const completedElement = createAnnotationElement(tool, draftPoints, settings);
    setDraftPoints(null);
    if (completedElement) onElementCompleted(completedElement);
  };

  return (
    <div ref={containerRef} className="photo-annotation-canvas-container">
      <canvas ref={canvasRef} className="photo-annotation-canvas"
              style={{ width: displaySize.width, height: displaySize.height }}
              aria-label="Zdjęcie do adnotacji - rysuj palcem, rysikiem albo myszą"
              onPointerDown={startDrawing} onPointerMove={continueDrawing}
              onPointerUp={finishDrawing} onPointerCancel={() => setDraftPoints(null)} />
    </div>
  );
}

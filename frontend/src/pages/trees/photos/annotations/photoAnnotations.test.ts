import assert from 'node:assert/strict';
import { test } from 'node:test';
import { toNormalizedPoint, toPixelPoint } from './annotationCoordinates.ts';
import type { AnnotationElement } from './annotationDataTypes.ts';
import { appendFreehandPoint, createAnnotationElement, toAnnotationDocument } from './annotationElements.ts';
import {
  addAnnotationElement, clearAnnotation, createAnnotationHistory, hasUnsavedAnnotationChanges, redoAnnotation,
  undoAnnotation,
} from './annotationHistory.ts';
import { drawAnnotationElements, type AnnotationDrawingContext } from './annotationRenderer.ts';

// Uruchamianie: npm test (node --test, bez dodatkowych zaleznosci). Bez prawdziwego canvasu - kontekst nagrywa wywolania.

const settings = { color: 'RED' as const, strokeSize: 'MEDIUM' as const };
const fixedId = () => 'element-1';

const elementWithId = (id: string): AnnotationElement =>
  ({ id, type: 'LINE', points: [{ x: 0, y: 0 }, { x: 1, y: 1 }], color: 'RED', strokeSize: 'SMALL' });

test('piksele -> znormalizowane -> piksele (rozne rozmiary wyswietlania)', () => {
  const imageRect = { left: 100, top: 50, width: 800, height: 600 };
  const point = toNormalizedPoint(300, 350, imageRect);
  assert.deepEqual(point, { x: 0.25, y: 0.5 });
  assert.deepEqual(toPixelPoint(point, 800, 600), { x: 200, y: 300 });
  // ten sam punkt na zdjeciu w pelnej rozdzielczosci
  assert.deepEqual(toPixelPoint(point, 4000, 3000), { x: 1000, y: 1500 });
});

test('punkt poza zdjeciem jest przycinany do 0..1, a wspolrzedne zaokraglane do 6 miejsc', () => {
  const imageRect = { left: 0, top: 0, width: 300, height: 300 };
  assert.deepEqual(toNormalizedPoint(-20, 999, imageRect), { x: 0, y: 1 });
  assert.equal(toNormalizedPoint(100, 0, imageRect).x, 0.333333);
});

test('LINE i ARROW: dokladnie 2 punkty (poczatek i koniec gestu), przypadkowe dotkniecie odrzucone', () => {
  const gesturePoints = [{ x: 0.1, y: 0.1 }, { x: 0.3, y: 0.2 }, { x: 0.5, y: 0.5 }];
  for (const type of ['LINE', 'ARROW'] as const) {
    assert.deepEqual(createAnnotationElement(type, gesturePoints, settings, '', fixedId), {
      id: 'element-1', type, color: 'RED', strokeSize: 'MEDIUM', points: [{ x: 0.1, y: 0.1 }, { x: 0.5, y: 0.5 }],
    });
    assert.equal(createAnnotationElement(type, [{ x: 0.1, y: 0.1 }, { x: 0.1001, y: 0.1 }], settings), null);
    assert.equal(createAnnotationElement(type, [{ x: 0.1, y: 0.1 }], settings), null);
  }
});

test('FREEHAND: od 2 punktow, punkty dopisywane tylko przy wyraznym ruchu', () => {
  assert.equal(createAnnotationElement('FREEHAND', [{ x: 0.2, y: 0.2 }], settings), null);
  let points = appendFreehandPoint([], { x: 0.2, y: 0.2 });
  points = appendFreehandPoint(points, { x: 0.2001, y: 0.2 }); // drgniecie - pominiete
  points = appendFreehandPoint(points, { x: 0.25, y: 0.2 });
  assert.equal(points.length, 2);
  assert.equal(createAnnotationElement('FREEHAND', points, settings, '', fixedId)?.points.length, 2);
});

test('FREEHAND: limit 1000 punktow', () => {
  let points: { x: number; y: number }[] = [];
  // zygzak z krokiem wyraznie wiekszym niz minimalny ruch
  for (let index = 0; index < 1200; index += 1) points = appendFreehandPoint(points, { x: index / 1200, y: index % 2 ? 0.5 : 0.6 });
  assert.equal(points.length, 1000);
});

test('TEXT: 1 punkt, tekst przyciety, pusty tekst odrzucony', () => {
  assert.deepEqual(createAnnotationElement('TEXT', [{ x: 0.4, y: 0.6 }], settings, '  Ciąć tutaj ', fixedId), {
    id: 'element-1', type: 'TEXT', color: 'RED', strokeSize: 'MEDIUM', points: [{ x: 0.4, y: 0.6 }], text: 'Ciąć tutaj',
  });
  assert.equal(createAnnotationElement('TEXT', [{ x: 0.4, y: 0.6 }], settings, '   '), null);
  assert.equal(createAnnotationElement('TEXT', [{ x: 0.4, y: 0.6 }], settings, 'x'.repeat(600))?.text?.length, 500);
});

test('dokument dla backendu: schemaVersion 1, tekst tylko przy TEXT', () => {
  const document = toAnnotationDocument([
    { ...elementWithId('a'), text: 'nie powinno trafic' },
    { id: 'b', type: 'TEXT', points: [{ x: 0.5, y: 0.5 }], color: 'WHITE', strokeSize: 'LARGE', text: 'Uwaga' },
  ]);
  assert.equal(document.schemaVersion, 1);
  assert.equal('text' in document.elements[0], false);
  assert.equal(document.elements[1].text, 'Uwaga');
  assert.deepEqual(JSON.parse(JSON.stringify(document)).elements[0],
    { id: 'a', type: 'LINE', points: [{ x: 0, y: 0 }, { x: 1, y: 1 }], color: 'RED', strokeSize: 'SMALL' });
});

test('identyfikatory elementow sa unikalne i w formacie akceptowanym przez backend', () => {
  const ids = new Set(Array.from({ length: 50 },
    () => createAnnotationElement('LINE', [{ x: 0, y: 0 }, { x: 1, y: 1 }], settings)?.id));
  assert.equal(ids.size, 50);
  for (const id of ids) assert.match(id ?? '', /^[A-Za-z0-9_-]{1,64}$/);
});

test('Cofnij / Ponów', () => {
  let history = createAnnotationHistory([]);
  history = addAnnotationElement(history, elementWithId('a'));
  history = addAnnotationElement(history, elementWithId('b'));
  history = undoAnnotation(history);
  assert.deepEqual(history.present.map((element) => element.id), ['a']);
  history = redoAnnotation(history);
  assert.deepEqual(history.present.map((element) => element.id), ['a', 'b']);
  // nowy element po cofnieciu czysci "Ponow"
  history = addAnnotationElement(undoAnnotation(history), elementWithId('c'));
  assert.deepEqual(history.present.map((element) => element.id), ['a', 'c']);
  assert.equal(history.future.length, 0);
  assert.equal(redoAnnotation(history), history);
  assert.equal(undoAnnotation(createAnnotationHistory([])).present.length, 0);
});

test('Wyczyść jest krokiem historii, ktory mozna cofnac', () => {
  let history = addAnnotationElement(createAnnotationHistory([]), elementWithId('a'));
  history = clearAnnotation(history);
  assert.equal(history.present.length, 0);
  assert.equal(clearAnnotation(history), history); // pusty rysunek - bez nowego kroku
  history = undoAnnotation(history);
  assert.deepEqual(history.present.map((element) => element.id), ['a']);
});

test('wykrywanie niezapisanych zmian wzgledem zapisanej adnotacji', () => {
  const saved = [elementWithId('a')];
  let history = createAnnotationHistory(saved);
  assert.equal(hasUnsavedAnnotationChanges(history, saved), false);
  history = addAnnotationElement(history, elementWithId('b'));
  assert.equal(hasUnsavedAnnotationChanges(history, saved), true);
  history = undoAnnotation(history);
  assert.equal(hasUnsavedAnnotationChanges(history, saved), false);
});

/** Kontekst nagrywajacy wspolrzedne rysowania - wystarczy do sprawdzenia skalowania bez prawdziwego canvasu. */
function recordingContext() {
  const calls: string[] = [];
  const context: AnnotationDrawingContext = {
    strokeStyle: '', fillStyle: '', lineWidth: 1, lineCap: 'butt', lineJoin: 'miter', font: '', textBaseline: 'alphabetic',
    save: () => {}, restore: () => {}, beginPath: () => {}, closePath: () => {}, stroke: () => calls.push('stroke'),
    fill: () => calls.push('fill'),
    moveTo: (x, y) => calls.push(`moveTo ${x},${y}`), lineTo: (x, y) => calls.push(`lineTo ${x},${y}`),
    strokeText: (text, x, y) => calls.push(`strokeText ${text} ${x},${y}`),
    fillText: (text, x, y) => calls.push(`fillText ${text} ${x},${y}`),
  };
  return { context, calls };
}

test('ten sam dokument rysuje sie proporcjonalnie w roznych wymiarach', () => {
  const elements: AnnotationElement[] = [
    { id: 'a', type: 'LINE', points: [{ x: 0.25, y: 0.5 }, { x: 0.75, y: 0.5 }], color: 'RED', strokeSize: 'MEDIUM' },
    { id: 't', type: 'TEXT', points: [{ x: 0.1, y: 0.2 }], color: 'YELLOW', strokeSize: 'SMALL', text: 'Uwaga' },
  ];
  const small = recordingContext();
  drawAnnotationElements(small.context, elements, 400, 300);
  assert.deepEqual(small.calls.slice(0, 2), ['moveTo 100,150', 'lineTo 300,150']);
  assert.ok(small.calls.includes('fillText Uwaga 40,60'));

  const large = recordingContext();
  drawAnnotationElements(large.context, elements, 4000, 3000);
  assert.deepEqual(large.calls.slice(0, 2), ['moveTo 1000,1500', 'lineTo 3000,1500']);
  assert.ok(large.calls.includes('fillText Uwaga 400,600'));
});

test('strzalka dostaje grot (wypelniony trojkat), linia nie', () => {
  const arrow = recordingContext();
  drawAnnotationElements(arrow.context,
    [{ id: 'a', type: 'ARROW', points: [{ x: 0, y: 0 }, { x: 1, y: 0 }], color: 'BLUE', strokeSize: 'LARGE' }], 100, 100);
  assert.ok(arrow.calls.includes('fill'));
  const line = recordingContext();
  drawAnnotationElements(line.context, [elementWithId('l')], 100, 100);
  assert.equal(line.calls.includes('fill'), false);
});

import { useEffect, useRef, useState } from 'react';
import { ApiRequestError } from '../../../../api/httpClient';
import { Alert } from '../../../../components/Alert';
import { LoadingSpinner } from '../../../../components/LoadingSpinner';
import { ModalDialog } from '../../../../components/ModalDialog';
import type { TreePhoto } from '../treePhotoDataTypes';
import { treePhotosApi } from '../treePhotosApi';
import { useTreePhotoObjectUrl } from '../useTreePhotoObjectUrl';
import type { AnnotationColor, AnnotationElement, AnnotationElementType, AnnotationStrokeSize } from './annotationDataTypes';
import { toAnnotationDocument } from './annotationElements';
import {
  addAnnotationElement, clearAnnotation, createAnnotationHistory, hasUnsavedAnnotationChanges, redoAnnotation,
  undoAnnotation, type AnnotationHistory,
} from './annotationHistory';
import { drawAnnotationElements } from './annotationRenderer';
import { PhotoAnnotationCanvas } from './PhotoAnnotationCanvas';
import { PhotoAnnotationToolbar } from './PhotoAnnotationToolbar';

/** Dluzszy bok splaszczonego PNG do dokumentacji - V1: 2560 px (rozsadny kompromis rozmiar/jakosc druku). */
const MAXIMUM_RENDERED_IMAGE_SIDE_PX = 2560;

/** Splaszczony PNG: oryginal + rysunek w rozdzielczosci zdjecia (ograniczonej), niezaleznie od rozmiaru ekranu. */
function renderAnnotatedPng(image: HTMLImageElement, elements: AnnotationElement[]): Promise<Blob> {
  const scale = Math.min(1, MAXIMUM_RENDERED_IMAGE_SIDE_PX / Math.max(image.naturalWidth, image.naturalHeight));
  const canvas = document.createElement('canvas');
  canvas.width = Math.round(image.naturalWidth * scale);
  canvas.height = Math.round(image.naturalHeight * scale);
  const context = canvas.getContext('2d');
  if (!context) return Promise.reject(new Error('Canvas 2D is not available'));
  context.drawImage(image, 0, 0, canvas.width, canvas.height);
  drawAnnotationElements(context, elements, canvas.width, canvas.height);
  return new Promise((resolve, reject) => canvas.toBlob(
    (pngBlob) => (pngBlob ? resolve(pngBlob) : reject(new Error('PNG export failed'))), 'image/png'));
}

type EditorLoadingState =
  | { status: 'loading' }
  | { status: 'failed'; errorMessage: string }
  | { status: 'ready'; image: HTMLImageElement; savedElements: AnnotationElement[] };

/**
 * Edytor adnotacji zdjecia (pelny ekran): szkic na zdjeciu - linia, strzalka, rysowanie odreczne, tekst, Cofnij/Ponow.
 * Oryginalne zdjecie nigdy nie jest zmieniane - zapis wysyla dokument wektorowy (wspolrzedne 0..1) i osobny
 * splaszczony PNG do dokumentacji. Nic nie zapisuje sie samo; "Anuluj" z niezapisanymi zmianami pyta o potwierdzenie.
 */
export function TreePhotoAnnotationDialog({ projectId, treeId, treePhoto, onAnnotationChanged, closeDialog }: {
  projectId: number;
  treeId: number;
  treePhoto: TreePhoto;
  onAnnotationChanged: (annotationState: { hasAnnotation: boolean; annotationUpdatedAt: string | null }) => void;
  closeDialog: () => void;
}) {
  const originalPhotoState = useTreePhotoObjectUrl(projectId, treeId, treePhoto.id, 'original');
  const [editorLoadingState, setEditorLoadingState] = useState<EditorLoadingState>({ status: 'loading' });
  const [history, setHistory] = useState<AnnotationHistory>(createAnnotationHistory([]));
  const [tool, setTool] = useState<AnnotationElementType>('FREEHAND');
  const [color, setColor] = useState<AnnotationColor>('RED');
  const [strokeSize, setStrokeSize] = useState<AnnotationStrokeSize>('MEDIUM');
  const [pendingText, setPendingText] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [hintMessage, setHintMessage] = useState<string | null>(null);
  const [isDiscardConfirmationVisible, setIsDiscardConfirmationVisible] = useState(false);
  const [isRemovalConfirmationVisible, setIsRemovalConfirmationVisible] = useState(false);
  // Synchroniczna blokada podwojnego wyslania - stan Reacta zmienia sie dopiero po renderze.
  const isSubmissionInProgress = useRef(false);

  // Oryginal jako <img> (z lokalnego blob: - canvas pozostaje "czysty" i mozna z niego zrobic PNG) + zapisana adnotacja.
  useEffect(() => {
    if (originalPhotoState.status === 'failed') {
      setEditorLoadingState({ status: 'failed', errorMessage: 'Nie udało się wczytać zdjęcia.' });
      return;
    }
    if (originalPhotoState.status !== 'loaded') return;
    let isResultStillWanted = true;
    const image = new Image();
    image.src = originalPhotoState.objectUrl;
    Promise.all([
      image.decode(),
      treePhoto.hasAnnotation ? treePhotosApi.getTreePhotoAnnotation(projectId, treeId, treePhoto.id) : Promise.resolve(null),
    ])
      .then(([, savedAnnotation]) => {
        if (!isResultStillWanted) return;
        const savedElements = savedAnnotation?.elements ?? [];
        setHistory(createAnnotationHistory(savedElements));
        setEditorLoadingState({ status: 'ready', image, savedElements });
      })
      .catch((loadingError) => {
        if (!isResultStillWanted) return;
        setEditorLoadingState({ status: 'failed', errorMessage: `Nie udało się wczytać adnotacji. ${
          loadingError instanceof ApiRequestError ? loadingError.message : ''}`.trim() });
      });
    return () => { isResultStillWanted = false; };
  }, [originalPhotoState, projectId, treeId, treePhoto.id, treePhoto.hasAnnotation]);

  const savedElements = editorLoadingState.status === 'ready' ? editorLoadingState.savedElements : [];
  const hasUnsavedChanges = hasUnsavedAnnotationChanges(history, savedElements);

  const requestClose = () => {
    if (isSaving) return;
    if (hasUnsavedChanges) setIsDiscardConfirmationVisible(true);
    else closeDialog();
  };

  const runSubmission = async (submission: () => Promise<void>) => {
    if (isSubmissionInProgress.current) return;
    isSubmissionInProgress.current = true;
    setIsSaving(true);
    setErrorMessage(null);
    try {
      await submission();
      closeDialog();
    } catch (savingError) {
      setErrorMessage(`Nie udało się zapisać adnotacji. ${
        savingError instanceof ApiRequestError ? savingError.message : 'Nieoczekiwany błąd.'}`);
      isSubmissionInProgress.current = false;
      setIsSaving(false);
    }
  };

  const saveAnnotation = () => runSubmission(async () => {
    if (editorLoadingState.status !== 'ready') return;
    const renderedImage = await renderAnnotatedPng(editorLoadingState.image, history.present);
    const savedAnnotation = await treePhotosApi.saveTreePhotoAnnotation(projectId, treeId, treePhoto.id,
      toAnnotationDocument(history.present), renderedImage);
    // Odpowiedz backendu jest zrodlem prawdy (m.in. data zmiany - odswieza miniature do dokumentacji).
    onAnnotationChanged({ hasAnnotation: true, annotationUpdatedAt: savedAnnotation.updatedAt });
  });

  const removeAnnotation = () => runSubmission(async () => {
    await treePhotosApi.deleteTreePhotoAnnotation(projectId, treeId, treePhoto.id);
    onAnnotationChanged({ hasAnnotation: false, annotationUpdatedAt: null });
  });

  const isEmpty = history.present.length === 0;

  return (
    <ModalDialog title={`Adnotacje: ${treePhoto.originalFileName}`} closeDialog={requestClose} isClosingBlocked={isSaving}
                 isFullScreen>
      <div className="photo-annotation-editor stack">
        {errorMessage && <Alert variant="error">{errorMessage}</Alert>}
        {editorLoadingState.status === 'loading' && <div className="center"><LoadingSpinner /></div>}
        {editorLoadingState.status === 'failed' && <Alert variant="error">{editorLoadingState.errorMessage}</Alert>}
        {editorLoadingState.status === 'ready' && (
          <>
            <PhotoAnnotationToolbar tool={tool} color={color} strokeSize={strokeSize} pendingText={pendingText}
                                    canUndo={history.past.length > 0} canRedo={history.future.length > 0}
                                    canClear={!isEmpty} disabled={isSaving}
                                    onToolChange={(newTool) => { setTool(newTool); setHintMessage(null); }}
                                    onColorChange={setColor} onStrokeSizeChange={setStrokeSize}
                                    onPendingTextChange={(text) => { setPendingText(text); setHintMessage(null); }}
                                    onUndo={() => setHistory(undoAnnotation)} onRedo={() => setHistory(redoAnnotation)}
                                    onClear={() => setHistory(clearAnnotation)} />
            {hintMessage && <Alert variant="info">{hintMessage}</Alert>}
            <PhotoAnnotationCanvas image={editorLoadingState.image} elements={history.present} tool={tool}
                                   settings={{ color, strokeSize }} pendingText={pendingText}
                                   onElementCompleted={(element) => {
                                     if (isSaving) return;
                                     setHistory((currentHistory) => addAnnotationElement(currentHistory, element));
                                   }}
                                   onTextToolWithoutText={() => setHintMessage('Najpierw wpisz tekst do wstawienia.')} />
            <p className="muted small">
              Oryginalne zdjęcie pozostaje bez zmian - adnotacja zapisuje się jako osobna warstwa i osobny obraz do dokumentacji.
            </p>
          </>
        )}

        {isDiscardConfirmationVisible ? (
          <div className="photo-annotation-confirmation" role="alert">
            <span>Masz niezapisane zmiany. Zamknąć bez zapisywania?</span>
            <div className="form-actions">
              <button type="button" className="btn btn-ghost" onClick={() => setIsDiscardConfirmationVisible(false)}>
                Wróć do edycji
              </button>
              <button type="button" className="btn btn-danger" onClick={closeDialog}>Zamknij bez zapisywania</button>
            </div>
          </div>
        ) : isRemovalConfirmationVisible ? (
          <div className="photo-annotation-confirmation" role="alert">
            <span>Czy na pewno chcesz usunąć adnotacje tego zdjęcia? Oryginalne zdjęcie zostanie.</span>
            <div className="form-actions">
              <button type="button" className="btn btn-ghost" onClick={() => setIsRemovalConfirmationVisible(false)} disabled={isSaving}>
                Anuluj
              </button>
              <button type="button" className="btn btn-danger" onClick={removeAnnotation} disabled={isSaving}>
                {isSaving ? 'Usuwanie…' : 'Usuń adnotacje'}
              </button>
            </div>
          </div>
        ) : (
          <div className="form-actions photo-annotation-actions">
            {treePhoto.hasAnnotation && (
              <button type="button" className="btn btn-danger-outline" disabled={isSaving}
                      onClick={() => setIsRemovalConfirmationVisible(true)}>
                Usuń adnotacje
              </button>
            )}
            <button type="button" className="btn btn-ghost" onClick={requestClose} disabled={isSaving}>Anuluj</button>
            <button type="button" className="btn btn-primary" onClick={saveAnnotation}
                    disabled={isSaving || isEmpty || !hasUnsavedChanges || editorLoadingState.status !== 'ready'}
                    title={isEmpty ? 'Narysuj coś albo użyj „Usuń adnotacje”.' : undefined}>
              {isSaving ? 'Zapisywanie…' : 'Zapisz'}
            </button>
          </div>
        )}
      </div>
    </ModalDialog>
  );
}

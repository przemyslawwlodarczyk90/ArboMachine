import { useState } from 'react';
import { LoadingSpinner } from '../../../components/LoadingSpinner';
import { useOfflineState } from '../../../offline/useOfflineState';
import type { TreePhoto } from './treePhotoDataTypes';
import { TreePhotoDocumentSettingsEditor } from './TreePhotoDocumentSettingsEditor';
import { useTreePhotoObjectUrl } from './useTreePhotoObjectUrl';

/**
 * Kafelek galerii: miniatura w wersji do dokumentacji (z adnotacja, jesli istnieje - pelne zdjecie przeskalowane
 * w CSS, backend nie generuje miniatur w V1), plakietka "Adnotowane", nazwa pliku, "Adnotuj"/"Edytuj adnotacje"
 * i "Usun". Klikniecie miniatury otwiera wiekszy podglad. Blad pobrania tresci pokazuje komunikat zamiast
 * "zepsutego" obrazka. Etap 52: podpis w dokumencie, plakietka "Poza dokumentem" i przycisk "Podpis".
 */
export function TreePhotoTile({ projectId, treeId, treePhoto, openPreview, openAnnotationEditor, requestRemoval,
                                onTreePhotoChanged }: {
  projectId: number;
  treeId: number;
  treePhoto: TreePhoto;
  openPreview: (treePhoto: TreePhoto, objectUrl: string) => void;
  openAnnotationEditor: (treePhoto: TreePhoto) => void;
  requestRemoval: (treePhoto: TreePhoto) => void;
  /** etap 52: zdjecie po zmianie podpisu / wyboru do dokumentu */
  onTreePhotoChanged: (changedTreePhoto: TreePhoto) => void;
}) {
  const [isDocumentSettingsEditorOpen, setIsDocumentSettingsEditorOpen] = useState(false);
  // Wersja (data adnotacji) w zaleznosciach: po zapisie adnotacji miniatura pobiera sie ponownie.
  const objectUrlState = useTreePhotoObjectUrl(projectId, treeId, treePhoto.id, 'documentation',
    treePhoto.annotationUpdatedAt ?? null);
  const { isOnline } = useOfflineState();

  return (
    <li className="tree-photo-tile">
      {objectUrlState.status === 'loaded' ? (
        <button type="button" className="tree-photo-thumbnail"
                onClick={() => openPreview(treePhoto, objectUrlState.objectUrl)}
                aria-label={`Powiększ zdjęcie ${treePhoto.originalFileName}`}>
          <img src={objectUrlState.objectUrl} alt={treePhoto.originalFileName} loading="lazy" />
        </button>
      ) : (
        <div className="tree-photo-thumbnail tree-photo-thumbnail-placeholder">
          {objectUrlState.status === 'loading'
            ? <LoadingSpinner />
            : <span className="muted small">{isOnline ? 'Nie udało się wczytać zdjęcia.' : 'Podgląd niedostępny bez zasięgu.'}</span>}
        </div>
      )}
      {treePhoto.hasAnnotation && <span className="badge tree-photo-annotated-badge">Adnotowane</span>}
      {treePhoto.includedInDocument === false && <span className="badge tree-photo-excluded-badge">Poza dokumentem</span>}
      {treePhoto.caption && <span className="tree-photo-caption small">{treePhoto.caption}</span>}
      {isDocumentSettingsEditorOpen && (
        <TreePhotoDocumentSettingsEditor projectId={projectId} treeId={treeId} treePhoto={treePhoto}
                                         onTreePhotoChanged={onTreePhotoChanged}
                                         closeEditor={() => setIsDocumentSettingsEditorOpen(false)} />
      )}
      <div className="tree-photo-tile-footer">
        <span className="tree-photo-tile-name small" title={treePhoto.originalFileName}>{treePhoto.originalFileName}</span>
        <div className="tree-photo-tile-actions">
          <button type="button" className="btn btn-secondary btn-small" onClick={() => openAnnotationEditor(treePhoto)}>
            {treePhoto.hasAnnotation ? 'Edytuj adnotacje' : 'Adnotuj'}
          </button>
          {!isDocumentSettingsEditorOpen && (
            <button type="button" className="btn btn-secondary btn-small" onClick={() => setIsDocumentSettingsEditorOpen(true)}>
              Podpis
            </button>
          )}
          <button type="button" className="btn btn-danger-outline btn-small" onClick={() => requestRemoval(treePhoto)}>
            Usuń
          </button>
        </div>
      </div>
    </li>
  );
}

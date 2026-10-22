import { useEffect, useState } from 'react';
import { offlineService } from '../../../offline/offlineService';
import { photoContentCacheKey } from '../../../offline/photoContentCache';
import { treePhotosApi } from './treePhotosApi';

export type TreePhotoObjectUrlState =
  | { status: 'loading' }
  | { status: 'loaded'; objectUrl: string }
  | { status: 'failed' };

/** Oryginal zdjecia albo wersja do dokumentacji (z adnotacja, jesli istnieje). */
export type TreePhotoContentKind = 'original' | 'documentation';

/**
 * Pobiera tresc zdjecia (z tokenem) i wystawia ja jako lokalny adres blob: do <img>. Adres jest zwalniany
 * (URL.revokeObjectURL) przy odmontowaniu albo zmianie zdjecia - bez wyciekow pamieci na tablecie.
 * contentVersion (np. data ostatniej adnotacji) wymusza ponowne pobranie po zmianie tresci.
 * Etap 40: pobrane zdjecie zostaje na urzadzeniu - ta sama wersja bez ponownego pobierania, bez zasiegu podglad
 * z ostatniej zapisanej wersji.
 */
export function useTreePhotoObjectUrl(projectId: number, treeId: number, photoId: number,
                                      contentKind: TreePhotoContentKind = 'original',
                                      contentVersion: string | null = null): TreePhotoObjectUrlState {
  const [objectUrlState, setObjectUrlState] = useState<TreePhotoObjectUrlState>({ status: 'loading' });

  useEffect(() => {
    let isResponseStillWanted = true;
    let createdObjectUrl: string | null = null;
    setObjectUrlState({ status: 'loading' });
    const loadContent = contentKind === 'documentation'
      ? treePhotosApi.getTreePhotoDocumentationContent
      : treePhotosApi.getTreePhotoContent;
    offlineService.loadPhotoContent(
      { key: photoContentCacheKey(projectId, treeId, photoId, contentKind), projectId, contentVersion },
      () => loadContent(projectId, treeId, photoId))
      .then((photoBlob) => {
        if (!isResponseStillWanted) return;
        createdObjectUrl = URL.createObjectURL(photoBlob);
        setObjectUrlState({ status: 'loaded', objectUrl: createdObjectUrl });
      })
      .catch(() => {
        if (isResponseStillWanted) setObjectUrlState({ status: 'failed' });
      });
    return () => {
      isResponseStillWanted = false;
      if (createdObjectUrl) URL.revokeObjectURL(createdObjectUrl);
    };
  }, [projectId, treeId, photoId, contentKind, contentVersion]);

  return objectUrlState;
}

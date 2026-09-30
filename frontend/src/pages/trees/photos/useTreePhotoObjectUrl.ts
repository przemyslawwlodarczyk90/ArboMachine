import { useEffect, useState } from 'react';
import { treePhotosApi } from './treePhotosApi';

export type TreePhotoObjectUrlState =
  | { status: 'loading' }
  | { status: 'loaded'; objectUrl: string }
  | { status: 'failed' };

/**
 * Pobiera tresc zdjecia (z tokenem) i wystawia ja jako lokalny adres blob: do <img>. Adres jest zwalniany
 * (URL.revokeObjectURL) przy odmontowaniu albo zmianie zdjecia - bez wyciekow pamieci na tablecie.
 */
export function useTreePhotoObjectUrl(projectId: number, treeId: number, photoId: number): TreePhotoObjectUrlState {
  const [objectUrlState, setObjectUrlState] = useState<TreePhotoObjectUrlState>({ status: 'loading' });

  useEffect(() => {
    let isResponseStillWanted = true;
    let createdObjectUrl: string | null = null;
    setObjectUrlState({ status: 'loading' });
    treePhotosApi.getTreePhotoContent(projectId, treeId, photoId)
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
  }, [projectId, treeId, photoId]);

  return objectUrlState;
}

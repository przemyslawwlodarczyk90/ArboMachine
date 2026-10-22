import { useEffect, useState } from 'react';
import type { PendingPhotoUpload } from '../../../offline/offlineTypes';
import { offlineService } from '../../../offline/offlineService';
import { formatFileSizeForDisplay } from './treePhotoFileRules';

/**
 * Kafelek zdjecia zapisanego na urzadzeniu i czekajacego na wyslanie (etap 33): miniatura z lokalnego pliku (Blob),
 * plakietka "Czeka na wysłanie" albo opis bledu serwera z "Ponów". "Usuń" kasuje zdjecie z urzadzenia (przed
 * wyslaniem). Adnotacje sa dostepne dopiero po wyslaniu - zdjecie musi miec id na serwerze.
 */
export function PendingTreePhotoTile({ pendingPhoto }: { pendingPhoto: PendingPhotoUpload }) {
  const [objectUrl, setObjectUrl] = useState<string | null>(null);
  const [isConfirmingRemoval, setIsConfirmingRemoval] = useState(false);

  useEffect(() => {
    const createdObjectUrl = URL.createObjectURL(pendingPhoto.blob);
    setObjectUrl(createdObjectUrl);
    return () => URL.revokeObjectURL(createdObjectUrl);
  }, [pendingPhoto.blob]);

  return (
    <li className="tree-photo-tile tree-photo-tile-pending">
      <div className="tree-photo-thumbnail tree-photo-thumbnail-placeholder">
        {objectUrl && <img src={objectUrl} alt={pendingPhoto.fileName} />}
      </div>
      {pendingPhoto.failure ? (
        <span className="badge offline-error-badge" role="status">
          Nie wysłano: {pendingPhoto.failure.message}
        </span>
      ) : (
        <span className="badge offline-pending-badge">Czeka na wysłanie</span>
      )}
      <div className="tree-photo-tile-footer">
        <span className="tree-photo-tile-name small" title={pendingPhoto.fileName}>
          {pendingPhoto.fileName} ({formatFileSizeForDisplay(pendingPhoto.sizeBytes)})
        </span>
        <div className="tree-photo-tile-actions">
          {pendingPhoto.failure && (
            <button type="button" className="btn btn-secondary btn-small"
                    onClick={() => void offlineService.retryPendingPhoto(pendingPhoto.clientUploadId)}>Ponów</button>
          )}
          {isConfirmingRemoval ? (
            <>
              <button type="button" className="btn btn-danger btn-small"
                      onClick={() => void offlineService.discardPendingPhoto(pendingPhoto.clientUploadId)}>Usuń z urządzenia</button>
              <button type="button" className="btn btn-ghost btn-small" onClick={() => setIsConfirmingRemoval(false)}>Anuluj</button>
            </>
          ) : (
            <button type="button" className="btn btn-danger-outline btn-small" onClick={() => setIsConfirmingRemoval(true)}>Usuń</button>
          )}
        </div>
      </div>
    </li>
  );
}

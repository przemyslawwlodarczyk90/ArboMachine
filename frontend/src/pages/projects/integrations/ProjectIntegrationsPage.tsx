import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { NotificationList } from '../../../notifications/NotificationList';
import { PushNotificationsPanel } from '../../../notifications/PushNotificationsPanel';
import { parsePositiveIdRouteParam } from '../../../routing/parsePositiveIdRouteParam';
import { dataExchangeApi, projectExportFileName } from '../dataexchange/dataExchangeApi';
import { saveBlobAsFile } from '../dataexchange/saveBlobAsFile';

/**
 * Sekcja "Integracje i powiadomienia" projektu (/projects/:projectId/integrations, etap 30): kalendarz prac (.ics)
 * do zaimportowania w Google / Outlook / Apple, powiadomienia push na tym urzadzeniu i ostatnie powiadomienia.
 * Wymiana danych z GIS jest w "Import i eksport". Integracje z systemami zewnetrznymi (API, urzedy) - do uzgodnienia.
 */
export function ProjectIntegrationsPage() {
  const projectId = parsePositiveIdRouteParam(useParams().projectId);
  const [isDownloading, setIsDownloading] = useState(false);
  const [downloadMessage, setDownloadMessage] = useState<{ variant: 'success' | 'error'; text: string } | null>(null);

  if (projectId === null) return null;

  const downloadCalendar = async () => {
    setIsDownloading(true);
    setDownloadMessage(null);
    try {
      saveBlobAsFile(await dataExchangeApi.downloadProjectExport(projectId, 'calendar-ics'), projectExportFileName(projectId, 'calendar-ics'));
      setDownloadMessage({ variant: 'success', text: 'Pobrano kalendarz prac. Otwórz plik w aplikacji kalendarza, aby go zaimportować.' });
    } catch (downloadError) {
      setDownloadMessage({ variant: 'error', text: `Nie udało się pobrać kalendarza. ${
        downloadError instanceof ApiRequestError ? downloadError.message : 'Nieoczekiwany błąd.'}` });
    } finally {
      setIsDownloading(false);
    }
  };

  return (
    <div className="stack">
      <h2>Integracje i powiadomienia</h2>

      <section className="stack-tight integrations-block">
        <h3>Kalendarz prac</h3>
        <p className="muted small">
          Plik .ics z pracami do wykonania (zalecenia i prace zaplanowane), które mają termin - każda jako wydarzenie
          całodniowe. Prace bez terminu, wykonane i anulowane są pomijane. Ponowny import aktualizuje wydarzenia
          zamiast je dublować. Terminy ustawiasz w karcie drzewa albo w <Link to="../management" relative="path">Gospodarce drzewostanem</Link>.
        </p>
        <div className="form-actions">
          <button type="button" className="btn btn-primary" onClick={() => void downloadCalendar()} disabled={isDownloading}>
            {isDownloading ? 'Pobieranie…' : 'Eksportuj do kalendarza (ICS)'}
          </button>
        </div>
        {downloadMessage && <Alert variant={downloadMessage.variant}>{downloadMessage.text}</Alert>}
      </section>

      <section className="stack-tight integrations-block">
        <h3>Powiadomienia</h3>
        <PushNotificationsPanel />
      </section>

      <section className="stack-tight integrations-block">
        <h3>Ostatnie powiadomienia</h3>
        <NotificationList />
      </section>
    </div>
  );
}

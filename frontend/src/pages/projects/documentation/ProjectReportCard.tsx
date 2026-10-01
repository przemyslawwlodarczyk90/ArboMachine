import { useEffect, useRef, useState } from 'react';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { projectDocumentsApi, projectReportFileName } from './projectDocumentsApi';

type ProjectReportState =
  | { status: 'idle' }
  | { status: 'generating' }
  | { status: 'ready'; objectUrl: string; generatedAt: Date }
  | { status: 'failed'; errorMessage: string };

/**
 * "Dokument projektu" (PDF, wersja MVP): generowanie na zadanie z aktualnych danych projektu. Po wygenerowaniu plik
 * pobiera sie automatycznie, a link "Pobierz dokument PDF" pozwala pobrac go ponownie bez kolejnego generowania.
 * Adres blob: jest zwalniany przy ponownym generowaniu i odmontowaniu.
 */
export function ProjectReportCard({ projectId }: { projectId: number }) {
  const [reportState, setReportState] = useState<ProjectReportState>({ status: 'idle' });
  const isGenerationInProgress = useRef(false);
  const currentObjectUrl = useRef<string | null>(null);

  useEffect(() => () => {
    if (currentObjectUrl.current) URL.revokeObjectURL(currentObjectUrl.current);
  }, []);

  const generateReport = async () => {
    if (isGenerationInProgress.current) return;
    isGenerationInProgress.current = true;
    setReportState({ status: 'generating' });
    try {
      const pdfBlob = await projectDocumentsApi.downloadProjectReport(projectId);
      if (currentObjectUrl.current) URL.revokeObjectURL(currentObjectUrl.current);
      const objectUrl = URL.createObjectURL(pdfBlob);
      currentObjectUrl.current = objectUrl;
      setReportState({ status: 'ready', objectUrl, generatedAt: new Date() });
      const downloadLink = document.createElement('a');
      downloadLink.href = objectUrl;
      downloadLink.download = projectReportFileName(projectId);
      downloadLink.click();
    } catch (generationError) {
      setReportState({
        status: 'failed',
        errorMessage: generationError instanceof ApiRequestError ? generationError.message : 'Nieoczekiwany błąd.',
      });
    } finally {
      isGenerationInProgress.current = false;
    }
  };

  const isGenerating = reportState.status === 'generating';

  return (
    <section className="project-report-card stack" aria-labelledby="project-report-heading">
      <h3 id="project-report-heading">Dokument projektu</h3>
      <p>
        Na podstawie danych projektu można wygenerować roboczy dokument PDF. Dokument zawiera aktualne dane projektu,
        drzew, zdjęć (w wersji z adnotacją, jeśli istnieje) oraz zapisanych prac.
      </p>
      <div className="form-actions project-report-actions">
        <button type="button" className="btn btn-primary" onClick={generateReport} disabled={isGenerating}
                aria-busy={isGenerating}>
          {isGenerating ? 'Generowanie dokumentu…' : 'Generuj dokument PDF'}
        </button>
        {reportState.status === 'ready' && (
          <a className="btn btn-secondary" href={reportState.objectUrl} download={projectReportFileName(projectId)}>
            Pobierz dokument PDF
          </a>
        )}
      </div>
      <div aria-live="polite">
        {reportState.status === 'ready' && (
          <Alert variant="success">
            Dokument został wygenerowany ({reportState.generatedAt.toLocaleTimeString('pl-PL', { hour: '2-digit', minute: '2-digit' })}).
          </Alert>
        )}
        {reportState.status === 'failed' && (
          <Alert variant="error">Nie udało się wygenerować dokumentu. {reportState.errorMessage}</Alert>
        )}
      </div>
      <p className="muted small">
        Wersja MVP — układ dokumentu będzie rozwijany na podstawie konsultacji z arborystami. Dane zleceniodawcy
        i autora są w dokumencie oznaczone jako „Dane do uzupełnienia”.
      </p>
    </section>
  );
}

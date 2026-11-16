import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ApiRequestError } from '../api/httpClient';
import { useAuthentication } from '../auth/AuthenticationContext';
import { Alert } from '../components/Alert';
import { demoApi } from './demoApi';

/**
 * Wejscie do trybu demo na stronie startowej (logowanie) - widoczne tylko, gdy backend ma wlaczone demo
 * (GET /api/demo). Jedno klikniecie: sesja konta demo i od razu lista projektow pokazowych.
 */
export function DemoEntrySection() {
  const { startDemoSession } = useAuthentication();
  const navigate = useNavigate();
  const [isAvailable, setIsAvailable] = useState(false);
  const [isStarting, setIsStarting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    let isStillWanted = true;
    demoApi.getStatus()
      .then((status) => { if (isStillWanted) setIsAvailable(status.enabled); })
      .catch(() => { if (isStillWanted) setIsAvailable(false); });
    return () => { isStillWanted = false; };
  }, []);

  if (!isAvailable) return null;

  const enterDemo = async () => {
    if (isStarting) return;
    setIsStarting(true);
    setErrorMessage(null);
    try {
      await startDemoSession();
      navigate('/projects', { replace: true });
    } catch (startError) {
      setErrorMessage(startError instanceof ApiRequestError ? startError.message : 'Nie udało się uruchomić trybu demo.');
      setIsStarting(false);
    }
  };

  return (
    <section className="demo-entry" aria-labelledby="demo-entry-heading">
      <div className="demo-entry-divider" aria-hidden="true"><span>albo</span></div>
      <h2 id="demo-entry-heading">Zobacz tryb demo</h2>
      <p className="muted small">
        Przeglądaj przykładowe projekty arborysty bez zakładania konta: mapy, karty drzew, zdjęcia z adnotacjami,
        plan prac i dokumenty. Dane są fikcyjne i tylko do odczytu.
      </p>
      {errorMessage && <Alert variant="error">{errorMessage}</Alert>}
      <button type="button" className="btn btn-secondary btn-block" onClick={() => void enterDemo()} disabled={isStarting}>
        {isStarting ? 'Uruchamianie demo…' : 'Wejdź do trybu demo (arborysta)'}
      </button>
    </section>
  );
}

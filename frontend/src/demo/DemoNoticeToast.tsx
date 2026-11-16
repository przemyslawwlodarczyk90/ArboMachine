import { useEffect } from 'react';
import { hideDemoNotice, useDemoNotice } from './demoNotice';

const NOTICE_VISIBLE_MS = 8000;

/** Krotkie wyjasnienie u dolu ekranu po kliknieciu zablokowanej funkcji demo. Nie blokuje ekranu, znika samo. */
export function DemoNoticeToast() {
  const { message, sequence } = useDemoNotice();

  useEffect(() => {
    if (!message) return;
    const timeoutId = window.setTimeout(hideDemoNotice, NOTICE_VISIBLE_MS);
    return () => window.clearTimeout(timeoutId);
  }, [message, sequence]);

  return (
    <div className="demo-notice-region" role="status" aria-live="polite">
      {message && (
        <div className="demo-notice" key={sequence}>
          <span className="demo-notice-badge">Tryb demo</span>
          <span className="demo-notice-text">{message}</span>
          <button type="button" className="demo-hint-close" aria-label="Zamknij komunikat" onClick={hideDemoNotice}>×</button>
        </div>
      )}
    </div>
  );
}

import { useDemoPreferences, demoPreferences } from './demoPreferences';
import { demoTour, useDemoTourState } from './demoTourStore';

/** Powitanie przy pierwszym wejsciu do demo - mala karta, ktora nie blokuje przegladania (bez tla). */
export function DemoWelcome() {
  const { welcomeSeen } = useDemoPreferences();
  const { isActive } = useDemoTourState();
  if (welcomeSeen || isActive) return null;

  const close = () => demoPreferences.markWelcomeSeen();
  return (
    <aside className="demo-floating-card demo-welcome" role="dialog" aria-labelledby="demo-welcome-title">
      <h2 id="demo-welcome-title">Witaj w trybie demo ArboMachine</h2>
      <p>
        To przykładowe projekty arborysty z fikcyjnymi danymi: inwentaryzacja parku, ocena stanu alei, dokumentacja
        fotograficzna i plan prac. Możesz wszystko przeglądać – zmiany nie są zapisywane.
      </p>
      <p className="muted small">Ikony <span className="demo-hint-inline-icon" aria-hidden="true">i</span> przy funkcjach wyjaśniają, do czego służą.</p>
      <div className="demo-floating-card-actions">
        <button type="button" className="btn btn-primary" onClick={() => { close(); demoTour.start(); }}>Rozpocznij oprowadzanie</button>
        <button type="button" className="btn btn-secondary" onClick={close}>Przeglądam samodzielnie</button>
      </div>
    </aside>
  );
}

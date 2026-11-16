import { useNavigate } from 'react-router-dom';
import { useAuthentication } from '../auth/AuthenticationContext';
import { DemoHint } from './DemoHint';
import { DemoTour } from './DemoTour';
import { DemoWelcome } from './DemoWelcome';
import { demoPreferences, useDemoPreferences } from './demoPreferences';
import { demoTour } from './demoTourStore';

/**
 * Pasek trybu demo nad trescia panelu (widok biurowy i tryb terenowy): wyjasnienie, "Oprowadź mnie", wlaczanie
 * wskazowek i wyjscie z demo. Renderuje tez powitanie i samouczek - poza demo nie renderuje niczego.
 */
export function DemoBar({ compact = false }: { compact?: boolean }) {
  const { isDemoSession, signOut } = useAuthentication();
  const { hintsEnabled } = useDemoPreferences();
  const navigate = useNavigate();
  if (!isDemoSession) return null;

  const endDemo = () => {
    demoTour.stop();
    signOut();
    navigate('/login', { replace: true });
  };

  return (
    <>
      <div className={`demo-bar${compact ? ' demo-bar-compact' : ''}`} role="region" aria-label="Tryb demo">
        <div className="demo-bar-info">
          <span className="demo-bar-badge">Tryb demo</span>
          <span className="demo-bar-text">Przykładowe, fikcyjne dane · tylko do odczytu</span>
          <DemoHint id="demo.about" alwaysVisible />
        </div>
        <div className="demo-bar-actions">
          <button type="button" className="btn btn-small btn-primary" onClick={demoTour.start}>Oprowadź mnie</button>
          <button type="button" className="btn btn-small btn-secondary" aria-pressed={hintsEnabled}
                  onClick={() => demoPreferences.setHintsEnabled(!hintsEnabled)}>
            Wskazówki: {hintsEnabled ? 'wł.' : 'wył.'}
          </button>
          <button type="button" className="btn btn-small btn-ghost" onClick={endDemo}>Zakończ demo</button>
        </div>
      </div>
      <DemoWelcome />
      <DemoTour />
    </>
  );
}

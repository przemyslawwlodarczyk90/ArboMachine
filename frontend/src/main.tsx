import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import { startOfflineSupport } from './offline/offlineService';
import { registerAppShellServiceWorker } from './offline/serviceWorkerRegistration';
import './styles.css';

// Praca offline (etap 26): kopie odczytow i kolejka zmian w IndexedDB, powloka aplikacji z service workera.
startOfflineSupport();
registerAppShellServiceWorker();

// StrictMode w trybie dev celowo odpala efekty dwa razy - stad ochrona w EmailConfirmationPage.
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);

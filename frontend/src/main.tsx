import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
import './styles.css';

// StrictMode w trybie dev celowo odpala efekty dwa razy - stad ochrona w EmailConfirmationPage.
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);

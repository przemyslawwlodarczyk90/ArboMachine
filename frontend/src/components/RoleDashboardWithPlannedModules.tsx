import { useEffect, useState, type ReactNode } from 'react';
import { ApiRequestError, arboMachineApi } from '../api/arboMachineApiClient';
import type { RolePanelWelcome, UserRole } from '../api/apiDataTypes';
import { Alert } from './Alert';
import { LoadingSpinner } from './LoadingSpinner';

export interface PlannedPanelModule {
  title: string;
  description: string;
}

/**
 * Wspolny pulpit arborysty i urzednika: powitanie z endpointu panelu danej roli (przy okazji
 * potwierdza, ze backend wpuszcza te role) oraz karty modulow planowanych dla tego panelu.
 */
export function RoleDashboardWithPlannedModules({ panelRole, introductionText, plannedModules, children }: {
  panelRole: UserRole;
  introductionText: string;
  plannedModules: PlannedPanelModule[];
  children?: ReactNode;
}) {
  const [panelWelcome, setPanelWelcome] = useState<RolePanelWelcome | null>(null);
  const [loadingErrorMessage, setLoadingErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    arboMachineApi.getRolePanelWelcome(panelRole)
      .then(setPanelWelcome)
      .catch((requestError) =>
        setLoadingErrorMessage(requestError instanceof ApiRequestError ? requestError.message : 'Unexpected error.'));
  }, [panelRole]);

  return (
    <div className="stack-lg">
      <header className="page-header">
        {panelWelcome ? <h1>Hello, {panelWelcome.firstName}</h1> : <h1>Dashboard</h1>}
        <p className="muted">{panelWelcome?.welcomeMessage ?? introductionText}</p>
      </header>

      {loadingErrorMessage && <Alert variant="error">{loadingErrorMessage}</Alert>}
      {!panelWelcome && !loadingErrorMessage && <div className="center"><LoadingSpinner /></div>}

      {children}

      <section>
        <h2 className="section-title">Coming soon</h2>
        <p className="muted small">{introductionText}</p>
        <div className="module-grid">
          {plannedModules.map((plannedModule) => (
            <article key={plannedModule.title} className="card module-card">
              <span className="badge badge-muted">Planned</span>
              <h3>{plannedModule.title}</h3>
              <p className="muted small">{plannedModule.description}</p>
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}

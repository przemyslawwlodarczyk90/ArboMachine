import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { OfflineStatusBar } from '../offline/OfflineStatusBar';
import type { UserRole } from '../api/apiDataTypes';
import { useAuthentication } from '../auth/AuthenticationContext';
import { PANEL_HOME_PATH_BY_ROLE, USER_ROLE_DISPLAY_NAMES } from '../auth/userRoles';
import { ArboMachineLogo } from './ArboMachineLogo';

interface PanelNavigationLink {
  path: string;
  label: string;
  /** podswietlaj tylko przy dokladnym dopasowaniu sciezki (dla strony glownej panelu) */
  matchExactPathOnly?: boolean;
}

/**
 * Arborysta: glowna zakladka to "Projekty" (strona startowa panelu) i profil. Etap 32: zakladki modulow "w budowie"
 * usuniete - klient ich nie zamawial.
 */
const NAVIGATION_LINKS_BY_ROLE: Record<UserRole, PanelNavigationLink[]> = {
  ARBORIST: [
    { path: '/projects', label: 'Projekty' },
    { path: '/arborist/profile', label: 'Mój profil' },
  ],
  OFFICIAL: [
    { path: '/official', label: 'Pulpit', matchExactPathOnly: true },
    { path: '/official/profile', label: 'Mój profil' },
  ],
  ADMIN: [
    { path: '/admin', label: 'Pulpit', matchExactPathOnly: true },
    { path: '/admin/users', label: 'Użytkownicy' },
    { path: '/admin/profile', label: 'Mój profil' },
  ],
};

/** Wspolny szkielet paneli: gorny pasek z logo, znacznikiem roli, menu danej roli i wylogowaniem. */
export function RolePanelLayout({ panelRole }: { panelRole: UserRole }) {
  const { currentUserAccount, signOut } = useAuthentication();
  const navigate = useNavigate();

  const signOutAndGoToSignInPage = () => {
    signOut();
    navigate('/login', { replace: true });
  };

  const roleCssSuffix = panelRole.toLowerCase();

  return (
    <div className={`app-shell panel-${roleCssSuffix}`}>
      <header className="topbar">
        <NavLink to={PANEL_HOME_PATH_BY_ROLE[panelRole]} className="brand">
          <ArboMachineLogo /> <span>ArboMachine</span>
        </NavLink>
        <span className={`role-pill role-${roleCssSuffix}`}>{USER_ROLE_DISPLAY_NAMES[panelRole]}</span>
        <nav className="nav" aria-label="Menu główne">
          {NAVIGATION_LINKS_BY_ROLE[panelRole].map((navigationLink) => (
            <NavLink key={navigationLink.path} to={navigationLink.path} end={navigationLink.matchExactPathOnly}>
              {navigationLink.label}
            </NavLink>
          ))}
        </nav>
        <div className="topbar-user">
          <span className="muted">{currentUserAccount?.firstName} {currentUserAccount?.lastName}</span>
          <button className="btn btn-ghost" onClick={signOutAndGoToSignInPage}>Wyloguj</button>
        </div>
      </header>
      <main className="content">
        <OfflineStatusBar />
        <Outlet />
      </main>
    </div>
  );
}

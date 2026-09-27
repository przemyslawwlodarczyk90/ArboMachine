import { Navigate, Outlet, useLocation } from 'react-router-dom';
import type { UserRole } from '../api/apiDataTypes';
import { useAuthentication } from './AuthenticationContext';
import { PANEL_HOME_PATH_BY_ROLE, resolvePanelPathAfterSignIn } from './userRoles';
import { LoadingSpinner } from '../components/LoadingSpinner';

/**
 * Wpuszcza tylko zalogowanego uzytkownika z podana rola. Niezalogowanego wysyla do logowania,
 * a uzytkownika z inna rola - do jego wlasnego panelu.
 */
export function RequireSignedInUserWithRole({ requiredRole }: { requiredRole: UserRole }) {
  const { currentUserAccount, isRestoringSession } = useAuthentication();
  const currentLocation = useLocation();
  if (isRestoringSession) return <LoadingSpinner fillsWholePage />;
  if (!currentUserAccount) return <Navigate to="/login" replace state={{ pathBeforeSignIn: currentLocation.pathname }} />;
  if (currentUserAccount.role !== requiredRole) return <Navigate to={PANEL_HOME_PATH_BY_ROLE[currentUserAccount.role]} replace />;
  return <Outlet />;
}

/** Strona "/" - zalogowanego wysyla do jego panelu, pozostalych do logowania. */
export function RedirectToPanelOrSignIn() {
  const { currentUserAccount, isRestoringSession } = useAuthentication();
  if (isRestoringSession) return <LoadingSpinner fillsWholePage />;
  return <Navigate to={currentUserAccount ? PANEL_HOME_PATH_BY_ROLE[currentUserAccount.role] : '/login'} replace />;
}

/** Strony dla niezalogowanych (logowanie, rejestracja) - zalogowanego od razu przenosi do jego panelu. */
export function RedirectSignedInUserToPanel() {
  const { currentUserAccount, isRestoringSession } = useAuthentication();
  const pathBeforeSignIn = (useLocation().state as { pathBeforeSignIn?: string } | null)?.pathBeforeSignIn;
  if (isRestoringSession) return <LoadingSpinner fillsWholePage />;
  if (currentUserAccount) return <Navigate to={resolvePanelPathAfterSignIn(currentUserAccount.role, pathBeforeSignIn)} replace />;
  return <Outlet />;
}

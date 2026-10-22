import { BrowserRouter, Link, Navigate, Route, Routes } from 'react-router-dom';
import { AuthenticationProvider } from './auth/AuthenticationContext';
import { RedirectSignedInUserToPanel, RedirectToPanelOrSignIn, RequireSignedInUserWithRole } from './auth/routeGuards';
import { AuthCardPageLayout } from './components/AuthCardPageLayout';
import { RolePanelLayout } from './components/RolePanelLayout';
import { AdminDashboardPage } from './pages/admin/AdminDashboardPage';
import { AdminUserAccountsPage } from './pages/admin/AdminUserAccountsPage';
import { EmailConfirmationPage } from './pages/EmailConfirmationPage';
import { FieldLayout } from './fieldmode/FieldLayout';
import { FieldProjectPage } from './fieldmode/FieldProjectPage';
import { FieldProjectsPage } from './fieldmode/FieldProjectsPage';
import { ForgotPasswordPage } from './pages/ForgotPasswordPage';
import { MyProfilePage } from './pages/MyProfilePage';
import { OfficialDashboardPage } from './pages/official/OfficialDashboardPage';
import { ProjectDataExchangePage } from './pages/projects/dataexchange/ProjectDataExchangePage';
import { ProjectDocumentationPage } from './pages/projects/documentation/ProjectDocumentationPage';
import { ProjectPage } from './pages/projects/ProjectPage';
import { ProjectsPage } from './pages/projects/ProjectsPage';
import { ProjectSummaryPage } from './pages/projects/summary/ProjectSummaryPage';
import { ProjectTreeMapSection } from './pages/trees/map/ProjectTreeMapSection';
import { ProjectTreesSection } from './pages/trees/ProjectTreesSection';
import { ProjectManagementPage } from './pages/projects/management/ProjectManagementPage';
import { ProjectIntegrationsPage } from './pages/projects/integrations/ProjectIntegrationsPage';
import { ProjectStandsPage } from './pages/projects/stands/ProjectStandsPage';
import { TreePage } from './pages/trees/TreePage';
import { RegistrationPage } from './pages/RegistrationPage';
import { ResetPasswordPage } from './pages/ResetPasswordPage';
import { SignInPage } from './pages/SignInPage';

/** Mapa tras aplikacji: strony publiczne, linki z maili i trzy panele - kazdy dostepny tylko dla swojej roli. */
export default function App() {
  return (
    <BrowserRouter>
      <AuthenticationProvider>
        <Routes>
          <Route index element={<RedirectToPanelOrSignIn />} />

          <Route element={<RedirectSignedInUserToPanel />}>
            <Route path="/login" element={<SignInPage />} />
            <Route path="/register" element={<RegistrationPage />} />
            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          </Route>

          {/* Linki z maili - dzialaja niezaleznie od tego, czy ktos jest zalogowany. */}
          <Route path="/confirm" element={<EmailConfirmationPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />

          <Route path="/arborist" element={<RequireSignedInUserWithRole requiredRole="ARBORIST" />}>
            <Route element={<RolePanelLayout panelRole="ARBORIST" />}>
              {/* Dawny pulpit: strona startowa arborysty to teraz lista projektow. */}
              <Route index element={<Navigate to="/projects" replace />} />
              <Route path="profile" element={<MyProfilePage />} />
              {/* Etap 32: dawne zakladki modulow "w budowie" (np. /arborist/harmonogram) i inne stare adresy -> projekty. */}
              <Route path="*" element={<Navigate to="/projects" replace />} />
            </Route>
          </Route>

          {/* Projekty arborysty - ten sam straznik roli i ten sam layout panelu co /arborist. */}
          <Route path="/projects" element={<RequireSignedInUserWithRole requiredRole="ARBORIST" />}>
            <Route element={<RolePanelLayout panelRole="ARBORIST" />}>
              <Route index element={<ProjectsPage />} />
              <Route path=":projectId" element={<ProjectPage />}>
                <Route index element={<ProjectSummaryPage />} />
                <Route path="trees" element={<ProjectTreesSection />} />
                <Route path="trees/:treeId" element={<TreePage />} />
                <Route path="map" element={<ProjectTreeMapSection />} />
                {/* Etap 29: grupy drzew (obszary). */}
                <Route path="stands" element={<ProjectStandsPage />} />
                {/* Etap 28: prace wszystkich drzew projektu z filtrami. */}
                <Route path="management" element={<ProjectManagementPage />} />
                {/* Dokument projektu (PDF) i materialy do dokumentacji (zdjecia projektu w wersji z adnotacja). */}
                <Route path="documentation" element={<ProjectDocumentationPage />} />
                <Route path="import-export" element={<ProjectDataExchangePage />} />
                {/* Etap 30: kalendarz prac (ICS) i powiadomienia. */}
                <Route path="integrations" element={<ProjectIntegrationsPage />} />
              </Route>
            </Route>
          </Route>

          {/* Etap 32: tryb terenowy arborysty - ten sam straznik roli, wlasny uklad (mapa na caly ekran, bez kolumny .content). */}
          <Route path="/field" element={<RequireSignedInUserWithRole requiredRole="ARBORIST" />}>
            <Route element={<FieldLayout />}>
              <Route index element={<FieldProjectsPage />} />
              <Route path="projects/:projectId" element={<FieldProjectPage />} />
              <Route path="*" element={<Navigate to="/field" replace />} />
            </Route>
          </Route>

          <Route path="/official" element={<RequireSignedInUserWithRole requiredRole="OFFICIAL" />}>
            <Route element={<RolePanelLayout panelRole="OFFICIAL" />}>
              <Route index element={<OfficialDashboardPage />} />
              <Route path="profile" element={<MyProfilePage />} />
            </Route>
          </Route>

          <Route path="/admin" element={<RequireSignedInUserWithRole requiredRole="ADMIN" />}>
            <Route element={<RolePanelLayout panelRole="ADMIN" />}>
              <Route index element={<AdminDashboardPage />} />
              <Route path="users" element={<AdminUserAccountsPage />} />
              <Route path="profile" element={<MyProfilePage />} />
            </Route>
          </Route>

          <Route path="*" element={
            <AuthCardPageLayout title="Nie znaleziono strony" footer={<Link to="/">Strona główna</Link>}>
              <p className="muted">Ta strona nie istnieje.</p>
            </AuthCardPageLayout>
          } />
        </Routes>
      </AuthenticationProvider>
    </BrowserRouter>
  );
}

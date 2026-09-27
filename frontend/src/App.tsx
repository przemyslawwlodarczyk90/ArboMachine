import { BrowserRouter, Link, Route, Routes } from 'react-router-dom';
import { AuthenticationProvider } from './auth/AuthenticationContext';
import { RedirectSignedInUserToPanel, RedirectToPanelOrSignIn, RequireSignedInUserWithRole } from './auth/routeGuards';
import { AuthCardPageLayout } from './components/AuthCardPageLayout';
import { RolePanelLayout } from './components/RolePanelLayout';
import { AdminDashboardPage } from './pages/admin/AdminDashboardPage';
import { AdminUserAccountsPage } from './pages/admin/AdminUserAccountsPage';
import { ArboristDashboardPage } from './pages/arborist/ArboristDashboardPage';
import { EmailConfirmationPage } from './pages/EmailConfirmationPage';
import { ForgotPasswordPage } from './pages/ForgotPasswordPage';
import { MyProfilePage } from './pages/MyProfilePage';
import { OfficialDashboardPage } from './pages/official/OfficialDashboardPage';
import { ProjectPage } from './pages/projects/ProjectPage';
import { ProjectsPage } from './pages/projects/ProjectsPage';
import { ProjectWorkspaceSectionPlaceholder } from './pages/projects/ProjectWorkspaceSectionPlaceholder';
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
              <Route index element={<ArboristDashboardPage />} />
              <Route path="profile" element={<MyProfilePage />} />
            </Route>
          </Route>

          {/* Projekty arborysty - ten sam straznik roli i ten sam layout panelu co /arborist. */}
          <Route path="/projects" element={<RequireSignedInUserWithRole requiredRole="ARBORIST" />}>
            <Route element={<RolePanelLayout panelRole="ARBORIST" />}>
              <Route index element={<ProjectsPage />} />
              <Route path=":projectId" element={<ProjectPage />}>
                <Route index element={<ProjectWorkspaceSectionPlaceholder sectionTitle="Trees"
                  plannedModuleMessage="The trees module will be added in the next stage." />} />
                <Route path="map" element={<ProjectWorkspaceSectionPlaceholder sectionTitle="Map"
                  plannedModuleMessage="The project map will be added in a later stage." />} />
                <Route path="documentation" element={<ProjectWorkspaceSectionPlaceholder sectionTitle="Photos & documentation"
                  plannedModuleMessage="Photos and documentation will be added in a later stage." />} />
              </Route>
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
            <AuthCardPageLayout title="Page not found" footer={<Link to="/">Home</Link>}>
              <p className="muted">This page does not exist.</p>
            </AuthCardPageLayout>
          } />
        </Routes>
      </AuthenticationProvider>
    </BrowserRouter>
  );
}

import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { ApiRequestError } from '../api/arboMachineApiClient';
import { useAuthentication } from '../auth/AuthenticationContext';
import { resolvePanelPathAfterSignIn } from '../auth/userRoles';
import { Alert } from '../components/Alert';
import { AuthCardPageLayout } from '../components/AuthCardPageLayout';
import { LabeledInputField } from '../components/LabeledInputField';

/** Stan przekazywany przy przekierowaniu na strone logowania. */
interface SignInPageNavigationState {
  /** strona, z ktorej wyrzucilo niezalogowanego uzytkownika */
  pathBeforeSignIn?: string;
  /** komunikat do pokazania, np. po zmianie hasla albo usunieciu konta */
  noticeMessage?: string;
}

export function SignInPage() {
  const { signIn } = useAuthentication();
  const navigate = useNavigate();
  const navigationState = (useLocation().state ?? {}) as SignInPageNavigationState;

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [signInError, setSignInError] = useState<ApiRequestError | null>(null);
  const [isSigningIn, setIsSigningIn] = useState(false);

  const submitSignIn = async (submitEvent: FormEvent) => {
    submitEvent.preventDefault();
    setSignInError(null);
    setIsSigningIn(true);
    try {
      const signedInAccount = await signIn(email.trim(), password);
      // Kazda rola ma swoj panel.
      navigate(resolvePanelPathAfterSignIn(signedInAccount.role, navigationState.pathBeforeSignIn), { replace: true });
    } catch (requestError) {
      setSignInError(requestError instanceof ApiRequestError ? requestError : new ApiRequestError(0, 'Unexpected error.'));
    } finally {
      setIsSigningIn(false);
    }
  };

  const emailIsNotConfirmedYet = signInError?.httpStatus === 403 && signInError.message.includes('not active');

  return (
    <AuthCardPageLayout
      title="Sign in"
      subtitle="Welcome back to ArboMachine."
      footer={<>No account yet? <Link to="/register">Create one</Link></>}
    >
      {navigationState.noticeMessage && !signInError && <Alert variant="success">{navigationState.noticeMessage}</Alert>}
      {signInError && (
        <Alert variant="error">
          {signInError.message}
          {emailIsNotConfirmedYet && (
            <> Didn't get the e-mail? <Link to="/register">Register again</Link> with the same address and we'll send a new link.</>
          )}
        </Alert>
      )}
      <form onSubmit={submitSignIn} className="form" noValidate>
        <LabeledInputField label="E-mail" type="email" autoComplete="email" required autoFocus value={email}
                           onChange={(changeEvent) => setEmail(changeEvent.target.value)}
                           errorMessage={signInError?.errorMessageByFieldName.email} />
        <LabeledInputField label="Password" type="password" autoComplete="current-password" required value={password}
                           onChange={(changeEvent) => setPassword(changeEvent.target.value)}
                           errorMessage={signInError?.errorMessageByFieldName.password} />
        <div className="form-row-end">
          <Link to="/forgot-password" className="small">Forgot your password?</Link>
        </div>
        <button type="submit" className="btn btn-primary btn-block" disabled={isSigningIn || !email || !password}>
          {isSigningIn ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
    </AuthCardPageLayout>
  );
}

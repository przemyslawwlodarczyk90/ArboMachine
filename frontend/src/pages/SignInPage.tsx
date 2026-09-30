import { useRef, useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { ApiRequestError } from '../api/httpClient';
import { toUserFacingErrorMessage } from '../api/userFacingErrorMessages';
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
  // Synchroniczna blokada podwojnego wyslania - stan Reacta zmienia sie dopiero po renderze.
  const isSignInInProgress = useRef(false);

  const submitSignIn = async (submitEvent: FormEvent) => {
    submitEvent.preventDefault();
    if (isSignInInProgress.current) return;
    isSignInInProgress.current = true;
    setSignInError(null);
    setIsSigningIn(true);
    try {
      const signedInAccount = await signIn(email.trim(), password);
      // Kazda rola ma swoj panel.
      navigate(resolvePanelPathAfterSignIn(signedInAccount.role, navigationState.pathBeforeSignIn), { replace: true });
    } catch (requestError) {
      setSignInError(requestError instanceof ApiRequestError ? requestError : new ApiRequestError(0, 'Nieoczekiwany błąd.'));
    } finally {
      isSignInInProgress.current = false;
      setIsSigningIn(false);
    }
  };

  const emailIsNotConfirmedYet = signInError?.httpStatus === 403
    && signInError.message === toUserFacingErrorMessage('User is not activated');

  return (
    <AuthCardPageLayout
      title="Logowanie"
      subtitle="Witaj ponownie w ArboMachine."
      footer={<>Nie masz konta? <Link to="/register">Zarejestruj się</Link></>}
    >
      {navigationState.noticeMessage && !signInError && <Alert variant="success">{navigationState.noticeMessage}</Alert>}
      {signInError && (
        <Alert variant="error">
          {signInError.message}
          {emailIsNotConfirmedYet && (
            <> Nie dotarł e-mail? <Link to="/register">Zarejestruj się ponownie</Link> na ten sam adres - wyślemy nowy link.</>
          )}
        </Alert>
      )}
      <form onSubmit={submitSignIn} className="form" noValidate>
        <LabeledInputField label="E-mail" type="email" autoComplete="email" required autoFocus value={email}
                           onChange={(changeEvent) => setEmail(changeEvent.target.value)}
                           errorMessage={signInError?.errorMessageByFieldName.email} />
        <LabeledInputField label="Hasło" type="password" autoComplete="current-password" required value={password}
                           onChange={(changeEvent) => setPassword(changeEvent.target.value)}
                           errorMessage={signInError?.errorMessageByFieldName.password} />
        <div className="form-row-end">
          <Link to="/forgot-password" className="small">Nie pamiętasz hasła?</Link>
        </div>
        <button type="submit" className="btn btn-primary btn-block" disabled={isSigningIn || !email || !password}>
          {isSigningIn ? 'Logowanie…' : 'Zaloguj się'}
        </button>
      </form>
    </AuthCardPageLayout>
  );
}

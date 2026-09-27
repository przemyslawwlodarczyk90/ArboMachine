import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { ArboMachineLogo } from './ArboMachineLogo';

/** Uklad stron logowania, rejestracji i linkow z maili: logo + wysrodkowana karta + stopka z linkiem. */
export function AuthCardPageLayout({ title, subtitle, children, footer }: {
  title: string;
  subtitle?: string;
  children: ReactNode;
  footer?: ReactNode;
}) {
  return (
    <div className="auth-page">
      <Link to="/" className="auth-brand"><ArboMachineLogo /> ArboMachine</Link>
      <main className="card auth-card">
        <h1>{title}</h1>
        {subtitle && <p className="muted">{subtitle}</p>}
        {children}
      </main>
      {footer && <div className="auth-footer">{footer}</div>}
    </div>
  );
}

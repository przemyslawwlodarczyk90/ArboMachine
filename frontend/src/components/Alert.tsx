import type { ReactNode } from 'react';

export type AlertVariant = 'error' | 'success' | 'info';

/** Kolorowy komunikat nad formularzem; bledy sa ogloszane czytnikom ekranu natychmiast (role="alert"). */
export function Alert({ variant = 'info', children }: { variant?: AlertVariant; children: ReactNode }) {
  return (
    <div className={`alert alert-${variant}`} role={variant === 'error' ? 'alert' : 'status'}>
      {children}
    </div>
  );
}

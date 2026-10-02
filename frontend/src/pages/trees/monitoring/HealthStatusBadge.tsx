import { healthStatusLabel, type TreeHealthStatus } from './monitoringDataTypes';

/** Ocena stanu z kontroli: kolor + zawsze nazwa (kolor nie jest jedynym nosnikiem informacji). Kolory umowne V1. */
export function HealthStatusBadge({ status, prefix = '' }: { status: TreeHealthStatus | null | undefined; prefix?: string }) {
  return (
    <span className={`badge health-status-badge health-status-${status ?? 'NONE'}`}>
      {prefix}{healthStatusLabel(status)}
    </span>
  );
}

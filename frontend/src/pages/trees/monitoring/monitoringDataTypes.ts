// Monitoring stanu drzew (etap 29) - typy odpowiadajace DTO z domain.treemonitoring.

/**
 * Ocena stanu przy kontroli V1 - TO NIE JEST SKALA BRANZOWA (nie VTA, nie klasa ryzyka); reczna ocena arborysty.
 * Brak (null) = "Nie okreslono". Ocena "zly" (POOR) wysyla powiadomienie.
 */
export type TreeHealthStatus = 'GOOD' | 'FAIR' | 'POOR';

export interface TreeMonitoringRecord {
  id: number;
  treeId: number;
  /** 'YYYY-MM-DD' */
  inspectionDate: string;
  healthStatus?: TreeHealthStatus | null;
  notes?: string | null;
  createdAt: string;
  updatedAt: string;
  /** tylko frontend (etap 26): zapisane na urzadzeniu, czeka na synchronizacje; id < 0 = wpis utworzony offline */
  offlinePending?: boolean;
}

/** Body POST/PUT - komplet pol (pominiete pole = brak wartosci). */
export interface TreeMonitoringRecordRequest {
  inspectionDate: string;
  healthStatus?: TreeHealthStatus;
  notes?: string;
}

export const HEALTH_STATUS_OPTIONS: { status: TreeHealthStatus; label: string }[] = [
  { status: 'GOOD', label: 'Dobry' },
  { status: 'FAIR', label: 'Umiarkowany' },
  { status: 'POOR', label: 'Zły' },
];

export const HEALTH_STATUS_NOT_SPECIFIED_LABEL = 'Nie określono';
export const NOT_INSPECTED_LABEL = 'Nie sprawdzone';

export const healthStatusLabel = (status: TreeHealthStatus | null | undefined) =>
  HEALTH_STATUS_OPTIONS.find((option) => option.status === status)?.label ?? HEALTH_STATUS_NOT_SPECIFIED_LABEL;

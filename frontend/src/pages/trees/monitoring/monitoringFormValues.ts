import type { TreeHealthStatus, TreeMonitoringRecord, TreeMonitoringRecordRequest } from './monitoringDataTypes.ts';

// Formularz kontroli stanu (etap 29) i filtr listy drzew po ostatniej kontroli. Walidacja w przegladarce tylko
// poprawia UX - backend waliduje ponownie. Modul bez Reacta - testowany przez node --test.

export const MAXIMUM_MONITORING_NOTES_LENGTH = 5000;

export interface MonitoringFormValues {
  /** 'YYYY-MM-DD' (input type="date") */
  inspectionDate: string;
  /** '' = nie okreslono */
  healthStatus: TreeHealthStatus | '';
  notes: string;
}

/** Nowa kontrola: dzisiejsza data (kontrole zwykle wpisuje sie w dniu ogledzin), bez oceny i uwag. */
export const emptyMonitoringFormValues = (today: string): MonitoringFormValues => ({ inspectionDate: today, healthStatus: '', notes: '' });

export const toMonitoringFormValues = (record: TreeMonitoringRecord): MonitoringFormValues => ({
  inspectionDate: record.inspectionDate,
  healthStatus: record.healthStatus ?? '',
  notes: record.notes ?? '',
});

export const toMonitoringRequest = (values: MonitoringFormValues): TreeMonitoringRecordRequest => ({
  inspectionDate: values.inspectionDate,
  healthStatus: values.healthStatus || undefined,
  notes: values.notes.trim() || undefined,
});

export const validateMonitoringFormValues = (values: MonitoringFormValues): Partial<Record<keyof MonitoringFormValues, string>> => {
  const errors: Partial<Record<keyof MonitoringFormValues, string>> = {};
  if (!values.inspectionDate) errors.inspectionDate = 'Podaj datę kontroli.';
  else if (!/^\d{4}-\d{2}-\d{2}$/.test(values.inspectionDate)) errors.inspectionDate = 'Podaj poprawną datę.';
  if (values.notes.trim().length > MAXIMUM_MONITORING_NOTES_LENGTH) {
    errors.notes = `Uwagi mogą mieć maksymalnie ${MAXIMUM_MONITORING_NOTES_LENGTH} znaków.`;
  }
  return errors;
};

export const areMonitoringFormValuesEquivalent = (first: MonitoringFormValues, second: MonitoringFormValues) =>
  JSON.stringify(toMonitoringRequest(first)) === JSON.stringify(toMonitoringRequest(second));

// ---------- lista drzew: ostatnia kontrola ----------

/** NOT_INSPECTED = drzewo bez kontroli; NOT_SPECIFIED = ostatnia kontrola bez oceny. */
export type MonitoringFilter = 'ALL' | 'NOT_INSPECTED' | 'NOT_SPECIFIED' | TreeHealthStatus;

export function latestRecordByTreeId(latestRecords: Pick<TreeMonitoringRecord, 'treeId' | 'inspectionDate' | 'healthStatus'>[]) {
  return new Map(latestRecords.map((record) => [record.treeId, record]));
}

export function filterTreesByMonitoring<T extends { id: number }>(
  trees: T[], latestByTreeId: Map<number, Pick<TreeMonitoringRecord, 'healthStatus'>>, filter: MonitoringFilter): T[] {
  if (filter === 'ALL') return trees;
  return trees.filter((tree) => {
    const latest = latestByTreeId.get(tree.id);
    if (filter === 'NOT_INSPECTED') return !latest;
    if (!latest) return false;
    return filter === 'NOT_SPECIFIED' ? !latest.healthStatus : latest.healthStatus === filter;
  });
}

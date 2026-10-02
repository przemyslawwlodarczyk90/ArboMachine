import type { TreeWork, TreeWorkPriority, TreeWorkStatus } from './treeWorkDataTypes.ts';

// Gospodarka drzewostanem V1 (etap 28): polskie nazwy statusow i priorytetow, filtr i kolejnosc prac projektu,
// stan drzewa na mapie. Modul bez React - testowany przez node --test. Nazwy i kolory sa robocze (do uzgodnienia).

export const TREE_WORK_STATUSES: TreeWorkStatus[] = ['RECOMMENDED', 'PLANNED', 'COMPLETED', 'CANCELLED'];
export const TREE_WORK_PRIORITIES: TreeWorkPriority[] = ['URGENT', 'HIGH', 'NORMAL', 'LOW'];

const STATUS_LABELS: Record<TreeWorkStatus, string> = {
  RECOMMENDED: 'Zalecenie', PLANNED: 'Zaplanowana', COMPLETED: 'Wykonana', CANCELLED: 'Anulowana',
};
const PRIORITY_LABELS: Record<TreeWorkPriority, string> = {
  LOW: 'Niski', NORMAL: 'Normalny', HIGH: 'Wysoki', URGENT: 'Pilny',
};

export const workStatusLabel = (status: TreeWorkStatus | null | undefined) => STATUS_LABELS[status ?? 'RECOMMENDED'];
export const workPriorityLabel = (priority: TreeWorkPriority | null | undefined) => (priority ? PRIORITY_LABELS[priority] : 'Nie określono');

/** Praca do wykonania = zalecona albo zaplanowana (jak TreeWorkStatus.isOpen() w backendzie). */
export const isOpenWorkStatus = (status: TreeWorkStatus | null | undefined) => {
  const effectiveStatus = status ?? 'RECOMMENDED';
  return effectiveStatus === 'RECOMMENDED' || effectiveStatus === 'PLANNED';
};

/** Dzisiejsza data lokalna jako 'YYYY-MM-DD' (input type="date", completedDate przy "Oznacz jako wykonaną"). */
export const todayIsoDate = (now: Date = new Date()) =>
  `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;

/** 'YYYY-MM-DD' -> 'DD.MM.YYYY' (bez stref czasowych - to data, nie chwila). */
export const formatWorkDate = (isoDate: string | null | undefined) => {
  const match = isoDate?.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  return match ? `${match[3]}.${match[2]}.${match[1]}` : '—';
};

/** Informacja (nie walidacja): praca do wykonania z terminem przed dzisiaj. Termin w przeszlosci jest dozwolony. */
export const isWorkPastPlannedDate = (work: Pick<TreeWork, 'status' | 'plannedDate'>, today: string) =>
  isOpenWorkStatus(work.status) && !!work.plannedDate && work.plannedDate < today;

// ---------- filtr widoku "Gospodarka drzewostanem" ----------

/** OPEN = do wykonania (zalecone + zaplanowane); NONE w priorytecie = bez priorytetu. */
export interface WorkFilter {
  status: 'ALL' | 'OPEN' | TreeWorkStatus;
  priority: 'ALL' | 'NONE' | TreeWorkPriority;
  treeId: 'ALL' | number;
  /** etap 30: tylko prace do wykonania z terminem w najblizszych UPCOMING_DAYS dniach (od dzis) */
  onlyUpcoming: boolean;
}

export const UPCOMING_DAYS = 7;

export const DEFAULT_WORK_FILTER: WorkFilter = { status: 'OPEN', priority: 'ALL', treeId: 'ALL', onlyUpcoming: false };

/** 'YYYY-MM-DD' + liczba dni (kalendarzowo, bez stref czasowych). */
export const addDaysToIsoDate = (isoDate: string, days: number) => {
  const [year, month, day] = isoDate.split('-').map(Number);
  return todayIsoDate(new Date(year, month - 1, day + days));
};

/** Praca do wykonania z terminem od dzis do dzis + UPCOMING_DAYS (wlacznie). */
export const isWorkUpcoming = (work: Pick<TreeWork, 'status' | 'plannedDate'>, today: string) =>
  isOpenWorkStatus(work.status) && !!work.plannedDate && work.plannedDate >= today
  && work.plannedDate <= addDaysToIsoDate(today, UPCOMING_DAYS);

export function filterWorks<T extends Pick<TreeWork, 'treeId' | 'status' | 'priority' | 'plannedDate'>>(works: T[], filter: WorkFilter,
                                                                                                     today: string = todayIsoDate()): T[] {
  return works.filter((work) =>
    (filter.status === 'ALL' || (filter.status === 'OPEN' ? isOpenWorkStatus(work.status) : (work.status ?? 'RECOMMENDED') === filter.status))
    && (filter.priority === 'ALL' || (filter.priority === 'NONE' ? !work.priority : work.priority === filter.priority))
    && (filter.treeId === 'ALL' || work.treeId === filter.treeId)
    && (!filter.onlyUpcoming || isWorkUpcoming(work, today)));
}

const PRIORITY_RANK: Record<string, number> = { URGENT: 0, HIGH: 1, NORMAL: 2, LOW: 3 };

/**
 * Kolejnosc listy prac: najpierw do wykonania, potem wg terminu (bez terminu na koncu), priorytetu (pilne pierwsze)
 * i id - stabilnie, bez zalezenia od kolejnosci z serwera.
 */
export function sortWorksForManagement<T extends Pick<TreeWork, 'id' | 'status' | 'priority' | 'plannedDate'>>(works: T[]): T[] {
  return [...works].sort((first, second) =>
    Number(!isOpenWorkStatus(first.status)) - Number(!isOpenWorkStatus(second.status))
    || (first.plannedDate ?? '9999-99-99').localeCompare(second.plannedDate ?? '9999-99-99')
    || (PRIORITY_RANK[first.priority ?? ''] ?? 4) - (PRIORITY_RANK[second.priority ?? ''] ?? 4)
    || first.id - second.id);
}

/** Liczniki nad lista: wszystkie, do wykonania i kazdy status. */
export function countWorks(works: Pick<TreeWork, 'status'>[]): Record<'ALL' | 'OPEN' | TreeWorkStatus, number> {
  const counts = { ALL: works.length, OPEN: 0, RECOMMENDED: 0, PLANNED: 0, COMPLETED: 0, CANCELLED: 0 };
  for (const work of works) {
    counts[work.status ?? 'RECOMMENDED']++;
    if (isOpenWorkStatus(work.status)) counts.OPEN++;
  }
  return counts;
}

// ---------- mapa: tryb "Gospodarka drzewostanem" ----------

/** URGENT_OPEN - jest pilna praca do wykonania; OPEN - inne prace do wykonania; CLOSED - tylko wykonane/anulowane. */
export type TreeManagementState = 'URGENT_OPEN' | 'OPEN' | 'CLOSED' | 'NO_WORKS';

export interface TreeManagementSummary {
  state: TreeManagementState;
  openWorkCount: number;
}

export const NO_WORKS_SUMMARY: TreeManagementSummary = { state: 'NO_WORKS', openWorkCount: 0 };

export function managementSummaryByTreeId(works: Pick<TreeWork, 'treeId' | 'status' | 'priority'>[]): Map<number, TreeManagementSummary> {
  const summaries = new Map<number, TreeManagementSummary>();
  for (const work of works) {
    const summary = summaries.get(work.treeId) ?? { state: 'CLOSED', openWorkCount: 0 };
    if (isOpenWorkStatus(work.status)) {
      summary.openWorkCount++;
      if (work.priority === 'URGENT') summary.state = 'URGENT_OPEN';
      else if (summary.state !== 'URGENT_OPEN') summary.state = 'OPEN';
    }
    summaries.set(work.treeId, summary);
  }
  return summaries;
}

/** Kolory celowo inne niz waloryzacja (zolcie/zielenie) - tryby mapy nie maja wspolnych kolorow. */
const MANAGEMENT_COLORS: Record<TreeManagementState, string> = {
  URGENT_OPEN: '#c92a2a', OPEN: '#1971c2', CLOSED: '#495057', NO_WORKS: '#ced4da',
};
const MANAGEMENT_LABELS: Record<TreeManagementState, string> = {
  URGENT_OPEN: 'Pilna praca do wykonania', OPEN: 'Prace do wykonania', CLOSED: 'Tylko prace wykonane lub anulowane', NO_WORKS: 'Bez prac',
};

export const managementColorOf = (state: TreeManagementState) => MANAGEMENT_COLORS[state];
export const managementStateLabel = (state: TreeManagementState) => MANAGEMENT_LABELS[state];

/** Legenda trybu: wszystkie stany (takze z zerem), z liczba drzew na mapie. */
export function buildManagementLegend(points: { management: TreeManagementSummary }[]): { state: TreeManagementState; count: number }[] {
  const states: TreeManagementState[] = ['URGENT_OPEN', 'OPEN', 'CLOSED', 'NO_WORKS'];
  return states.map((state) => ({ state, count: points.filter((point) => point.management.state === state).length }));
}

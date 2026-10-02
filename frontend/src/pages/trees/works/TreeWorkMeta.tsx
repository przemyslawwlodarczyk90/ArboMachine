import type { TreeWork } from './treeWorkDataTypes';
import { formatWorkDate, isWorkPastPlannedDate, todayIsoDate, workPriorityLabel, workStatusLabel } from './workManagement';

/**
 * Status, priorytet i terminy pracy (etap 28) - wspolne dla karty drzewa i widoku "Gospodarka drzewostanem".
 * Status ma kolor i zawsze tez nazwe; "po terminie" to tylko informacja (termin w przeszlosci jest dozwolony).
 */
export function TreeWorkMeta({ treeWork }: { treeWork: Pick<TreeWork, 'status' | 'priority' | 'plannedDate' | 'completedDate'> }) {
  const status = treeWork.status ?? 'RECOMMENDED';
  return (
    <span className="work-meta">
      <span className={`badge work-status-badge work-status-${status}`}>{workStatusLabel(status)}</span>
      {treeWork.priority && (
        <span className={`work-priority-${treeWork.priority}`}>Priorytet: {workPriorityLabel(treeWork.priority)}</span>
      )}
      {treeWork.plannedDate && (
        <span className={isWorkPastPlannedDate(treeWork, todayIsoDate()) ? 'work-overdue' : undefined}>
          Termin: {formatWorkDate(treeWork.plannedDate)}
          {isWorkPastPlannedDate(treeWork, todayIsoDate()) && ' (po terminie)'}
        </span>
      )}
      {treeWork.completedDate && <span>Wykonano: {formatWorkDate(treeWork.completedDate)}</span>}
    </span>
  );
}

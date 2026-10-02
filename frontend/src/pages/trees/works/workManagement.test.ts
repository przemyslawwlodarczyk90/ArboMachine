import assert from 'node:assert/strict';
import { test } from 'node:test';
import {
  addDaysToIsoDate, buildManagementLegend, countWorks, DEFAULT_WORK_FILTER, filterWorks, isWorkUpcoming, formatWorkDate, isOpenWorkStatus, isWorkPastPlannedDate,
  managementSummaryByTreeId, sortWorksForManagement, todayIsoDate, workPriorityLabel, workStatusLabel,
} from './workManagement.ts';
import type { TreeWorkPriority, TreeWorkStatus } from './treeWorkDataTypes.ts';

// Uruchamianie: npm test (node --test).

const work = (id: number, treeId: number, status: TreeWorkStatus, priority: TreeWorkPriority | null = null,
              plannedDate: string | null = null) => ({ id, treeId, status, priority, plannedDate });

const works = [
  work(1, 10, 'RECOMMENDED'),
  work(2, 10, 'PLANNED', 'URGENT', '2026-11-15'),
  work(3, 11, 'COMPLETED', 'URGENT'),
  work(4, 11, 'CANCELLED'),
  work(5, 12, 'PLANNED', 'LOW', '2026-10-20'),
];

test('polskie nazwy statusow i priorytetow; brak statusu = zalecenie', () => {
  const statuses: TreeWorkStatus[] = ['RECOMMENDED', 'PLANNED', 'COMPLETED', 'CANCELLED'];
  assert.deepEqual(statuses.map(workStatusLabel), ['Zalecenie', 'Zaplanowana', 'Wykonana', 'Anulowana']);
  assert.equal(workStatusLabel(null), 'Zalecenie');
  assert.equal(workPriorityLabel('URGENT'), 'Pilny');
  assert.equal(workPriorityLabel(null), 'Nie określono');
  assert.ok(isOpenWorkStatus('RECOMMENDED') && isOpenWorkStatus('PLANNED') && isOpenWorkStatus(null));
  assert.ok(!isOpenWorkStatus('COMPLETED') && !isOpenWorkStatus('CANCELLED'));
});

test('filtr: domyslnie prace do wykonania; status, priorytet (takze brak priorytetu) i drzewo', () => {
  assert.deepEqual(filterWorks(works, DEFAULT_WORK_FILTER).map((item) => item.id), [1, 2, 5]);
  assert.deepEqual(filterWorks(works, { status: 'ALL', priority: 'URGENT', treeId: 'ALL', onlyUpcoming: false }).map((item) => item.id), [2, 3]);
  assert.deepEqual(filterWorks(works, { status: 'ALL', priority: 'NONE', treeId: 'ALL', onlyUpcoming: false }).map((item) => item.id), [1, 4]);
  assert.deepEqual(filterWorks(works, { status: 'COMPLETED', priority: 'ALL', treeId: 'ALL', onlyUpcoming: false }).map((item) => item.id), [3]);
  assert.deepEqual(filterWorks(works, { status: 'ALL', priority: 'ALL', treeId: 11, onlyUpcoming: false }).map((item) => item.id), [3, 4]);
});

test('kolejnosc: do wykonania najpierw, potem termin, priorytet, id', () => {
  assert.deepEqual(sortWorksForManagement(works).map((item) => item.id), [5, 2, 1, 3, 4]);
});

test('liczniki statusow', () => {
  assert.deepEqual(countWorks(works), { ALL: 5, OPEN: 3, RECOMMENDED: 1, PLANNED: 2, COMPLETED: 1, CANCELLED: 1 });
});

test('daty: format polski, dzisiejsza data lokalna, termin w przeszlosci tylko jako informacja', () => {
  assert.equal(formatWorkDate('2026-11-05'), '05.11.2026');
  assert.equal(formatWorkDate(null), '—');
  assert.equal(todayIsoDate(new Date(2026, 0, 7, 23, 30)), '2026-01-07');
  assert.ok(isWorkPastPlannedDate(work(1, 1, 'PLANNED', null, '2026-09-30'), '2026-10-01'));
  assert.ok(!isWorkPastPlannedDate(work(1, 1, 'COMPLETED', null, '2026-09-30'), '2026-10-01'), 'wykonana nie jest "po terminie"');
  assert.ok(!isWorkPastPlannedDate(work(1, 1, 'PLANNED', null, '2026-10-01'), '2026-10-01'));
});

test('stan drzewa na mapie: pilna praca do wykonania > prace do wykonania > tylko zamkniete; legenda z zerami', () => {
  const summaries = managementSummaryByTreeId(works);
  assert.deepEqual(summaries.get(10), { state: 'URGENT_OPEN', openWorkCount: 2 });
  assert.deepEqual(summaries.get(11), { state: 'CLOSED', openWorkCount: 0 }, 'pilna, ale wykonana - nie jest pilna do wykonania');
  assert.deepEqual(summaries.get(12), { state: 'OPEN', openWorkCount: 1 });
  assert.equal(summaries.get(13), undefined);
  const legend = buildManagementLegend([...summaries.values()].map((management) => ({ management })));
  assert.deepEqual(legend.map((entry) => entry.count), [1, 1, 1, 0]);
});

test('etap 30: najblizsze terminy - prace do wykonania z terminem od dzis do dzis + 7 dni', () => {
  assert.equal(addDaysToIsoDate('2026-12-28', 7), '2027-01-04');
  assert.equal(addDaysToIsoDate('2026-02-25', 7), '2026-03-04');
  const today = '2026-10-02';
  assert.ok(isWorkUpcoming({ status: 'PLANNED', plannedDate: '2026-10-02' }, today));
  assert.ok(isWorkUpcoming({ status: 'RECOMMENDED', plannedDate: '2026-10-09' }, today));
  assert.ok(!isWorkUpcoming({ status: 'PLANNED', plannedDate: '2026-10-10' }, today));
  assert.ok(!isWorkUpcoming({ status: 'PLANNED', plannedDate: '2026-10-01' }, today), 'po terminie to nie "najblizsze"');
  assert.ok(!isWorkUpcoming({ status: 'COMPLETED', plannedDate: '2026-10-03' }, today));
  assert.ok(!isWorkUpcoming({ status: 'PLANNED', plannedDate: null }, today));
  const upcomingWorks = [
    { id: 1, treeId: 1, status: 'PLANNED' as const, priority: null, plannedDate: '2026-10-05' },
    { id: 2, treeId: 1, status: 'PLANNED' as const, priority: null, plannedDate: '2026-11-05' },
  ];
  assert.deepEqual(filterWorks(upcomingWorks, { ...DEFAULT_WORK_FILTER, onlyUpcoming: true }, today).map((item) => item.id), [1]);
});

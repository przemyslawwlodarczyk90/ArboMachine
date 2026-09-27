import { RoleDashboardWithPlannedModules, type PlannedPanelModule } from '../../components/RoleDashboardWithPlannedModules';

/** Moduly planowane w panelu arborysty - na razie tylko zapowiedzi na pulpicie. */
const ARBORIST_PLANNED_MODULES: PlannedPanelModule[] = [
  { title: 'Tree inspections', description: 'Record visual tree assessments, health condition and risk level.' },
  { title: 'Work reports', description: 'Document pruning, felling and other completed works.' },
  { title: 'Applications', description: 'Prepare and submit applications to the office.' },
  { title: 'Schedule', description: 'Plan site visits and upcoming jobs.' },
];

export function ArboristDashboardPage() {
  return (
    <RoleDashboardWithPlannedModules
      panelRole="ARBORIST"
      introductionText="Your arborist workspace. The modules below are planned and will appear here."
      plannedModules={ARBORIST_PLANNED_MODULES}
    />
  );
}

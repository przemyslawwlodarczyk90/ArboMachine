import { RoleDashboardWithPlannedModules, type PlannedPanelModule } from '../../components/RoleDashboardWithPlannedModules';
import { officialPanelApi } from './officialPanelApi';

/** Moduly planowane w panelu urzednika - na razie tylko zapowiedzi na pulpicie. */
const OFFICIAL_PLANNED_MODULES: PlannedPanelModule[] = [
  { title: 'Incoming applications', description: 'Review applications submitted by arborists.' },
  { title: 'Decisions & permits', description: 'Issue decisions and tree removal permits.' },
  { title: 'Tree register', description: 'Browse the register of inspected trees in your area.' },
  { title: 'Reports', description: 'Summaries and statistics for your office.' },
];

export function OfficialDashboardPage() {
  return (
    <RoleDashboardWithPlannedModules
      loadPanelWelcome={officialPanelApi.getOfficialPanelWelcome}
      introductionText="Your office workspace. The modules below are planned and will appear here."
      plannedModules={OFFICIAL_PLANNED_MODULES}
    />
  );
}

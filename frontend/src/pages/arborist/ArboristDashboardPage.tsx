import { RoleDashboardWithPlannedModules, type PlannedPanelModule } from '../../components/RoleDashboardWithPlannedModules';
import { arboristPanelApi } from './arboristPanelApi';

/** Moduly planowane w panelu arborysty - na razie tylko zapowiedzi na pulpicie. */
const ARBORIST_PLANNED_MODULES: PlannedPanelModule[] = [
  { title: 'Oceny drzew', description: 'Zapis oceny wizualnej drzew, stanu zdrowotnego i poziomu ryzyka.' },
  { title: 'Raporty z prac', description: 'Dokumentowanie cięć, wycinek i innych wykonanych prac.' },
  { title: 'Wnioski', description: 'Przygotowanie i składanie wniosków do urzędu.' },
  { title: 'Harmonogram', description: 'Planowanie wizyt w terenie i kolejnych zleceń.' },
];

export function ArboristDashboardPage() {
  return (
    <RoleDashboardWithPlannedModules
      loadPanelWelcome={arboristPanelApi.getArboristPanelWelcome}
      introductionText="Twoje miejsce pracy arborysty. Poniższe moduły są planowane i pojawią się tutaj."
      plannedModules={ARBORIST_PLANNED_MODULES}
    />
  );
}

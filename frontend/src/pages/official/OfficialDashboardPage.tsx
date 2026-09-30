import { RoleDashboardWithPlannedModules, type PlannedPanelModule } from '../../components/RoleDashboardWithPlannedModules';
import { officialPanelApi } from './officialPanelApi';

/** Moduly planowane w panelu urzednika - na razie tylko zapowiedzi na pulpicie. */
const OFFICIAL_PLANNED_MODULES: PlannedPanelModule[] = [
  { title: 'Wnioski przychodzące', description: 'Przeglądanie wniosków złożonych przez arborystów.' },
  { title: 'Decyzje i zezwolenia', description: 'Wydawanie decyzji i zezwoleń na usunięcie drzew.' },
  { title: 'Rejestr drzew', description: 'Przeglądanie rejestru zinwentaryzowanych drzew na Twoim terenie.' },
  { title: 'Raporty', description: 'Zestawienia i statystyki dla urzędu.' },
];

export function OfficialDashboardPage() {
  return (
    <RoleDashboardWithPlannedModules
      loadPanelWelcome={officialPanelApi.getOfficialPanelWelcome}
      introductionText="Twoje miejsce pracy w urzędzie. Poniższe moduły są planowane i pojawią się tutaj."
      plannedModules={OFFICIAL_PLANNED_MODULES}
    />
  );
}

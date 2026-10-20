import type { PlannedModuleIcon as IconName } from './plannedModules';

// Proste ikony liniowe modulow w budowie (inline SVG, kolor z currentColor - dziala w jasnym i ciemnym motywie).
const PATHS: Record<IconName, string[]> = {
  assessment: ['M9 3h6v3H9z', 'M7 5H5v16h14V5h-2', 'M8 13l3 3 5-6'],
  report: ['M6 3h9l4 4v14H6z', 'M14 3v5h5', 'M9 12h7', 'M9 16h7'],
  application: ['M5 4h14v16H5z', 'M8 8h8', 'M8 12h8', 'M8 16h4', 'M15 16l1.5 1.5L19 15'],
  calendar: ['M4 6h16v14H4z', 'M4 10h16', 'M8 3v5', 'M16 3v5', 'M8 14h3v3H8z'],
};

export function PlannedModuleIcon({ icon, size = 24 }: { icon: IconName; size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"
         strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {PATHS[icon].map((path) => <path key={path} d={path} />)}
    </svg>
  );
}

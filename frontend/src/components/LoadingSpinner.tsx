/** Krecace sie kolko; fillsWholePage = wysrodkowane na calym ekranie (np. przy przywracaniu sesji). */
export function LoadingSpinner({ fillsWholePage = false }: { fillsWholePage?: boolean }) {
  const spinner = <span className="spinner" role="status" aria-label="Ładowanie" />;
  return fillsWholePage ? <div className="page-center">{spinner}</div> : spinner;
}

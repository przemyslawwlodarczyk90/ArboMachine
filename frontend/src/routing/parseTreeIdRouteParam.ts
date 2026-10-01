/**
 * Id drzewa z trasy (:treeId): dodatnie id z serwera albo ujemne tymczasowe id drzewa utworzonego bez polaczenia
 * (etap 26 - istnieje tylko na urzadzeniu do synchronizacji). Cokolwiek innego (np. "abc", "0", "1.5") daje null.
 */
export function parseTreeIdRouteParam(routeParamValue: string | undefined): number | null {
  if (!routeParamValue || !/^-?\d+$/.test(routeParamValue)) return null;
  const parsedId = Number(routeParamValue);
  return Number.isSafeInteger(parsedId) && parsedId !== 0 ? parsedId : null;
}

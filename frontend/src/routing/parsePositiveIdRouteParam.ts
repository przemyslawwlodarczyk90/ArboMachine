/**
 * Id z parametru trasy (np. :projectId, :treeId). Cokolwiek innego niz dodatnia liczba calkowita
 * (np. "abc", "-1", "1.5") daje null - strona traktuje to jak nieistniejacy zasob, bez pytania backendu.
 */
export function parsePositiveIdRouteParam(routeParamValue: string | undefined): number | null {
  if (!routeParamValue || !/^\d+$/.test(routeParamValue)) return null;
  const parsedId = Number(routeParamValue);
  return Number.isSafeInteger(parsedId) && parsedId > 0 ? parsedId : null;
}

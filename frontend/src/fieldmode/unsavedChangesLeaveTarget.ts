// Etap 46: ktore klikniecie w link opuszcza ekran z niezapisanymi zmianami i ma zapytac o decyzje.
// Modul bez React - testowany przez node --test.

export interface LinkClick {
  /** atrybut href linku (moze byc wzgledny) */
  href: string | null;
  target: string | null;
  hasDownloadAttribute: boolean;
  /** Ctrl/Cmd/Shift/Alt albo przycisk inny niz lewy - przegladarka otworzy nowa karte, ten ekran zostaje */
  opensElsewhere: boolean;
}

/**
 * Adres w aplikacji (sciezka + zapytanie), do ktorego prowadzi klikniecie, albo null, gdy klikniecie nie opuszcza
 * biezacego ekranu w tej karcie: link zewnetrzny, nowa karta, pobieranie, kotwica albo ten sam adres.
 */
export function internalLeaveTarget(click: LinkClick, currentUrl: string): string | null {
  if (!click.href || click.opensElsewhere || click.hasDownloadAttribute) return null;
  if (click.target && click.target !== '_self') return null;
  let destination: URL;
  let current: URL;
  try {
    current = new URL(currentUrl);
    destination = new URL(click.href, current);
  } catch {
    return null;
  }
  if (destination.origin !== current.origin) return null;
  if (destination.pathname === current.pathname && destination.search === current.search) return null;
  return destination.pathname + destination.search + destination.hash;
}

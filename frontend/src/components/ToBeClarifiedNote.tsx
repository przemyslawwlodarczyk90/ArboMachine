import type { ReactNode } from 'react';

/**
 * Znacznik "Do doprecyzowania" w miejscu braku w UI + krotki komentarz, co trzeba ustalic z arborysta.
 * Funkcja dziala w wersji V1 albo jest tylko zaslepka - pelna lista otwartych pytan: docs/DO_DOPRECYZOWANIA.md.
 */
export function ToBeClarifiedNote({ children }: { children: ReactNode }) {
  return (
    <p className="to-be-clarified">
      <span className="to-be-clarified-badge">Do doprecyzowania</span>
      <span>{children}</span>
    </p>
  );
}

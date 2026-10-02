// Grupa gatunku (etap 29): iglaste / lisciaste - z nazwy gatunku, do ksztaltu markera na mapie (gwiazdka / kolko)
// i filtra listy drzew. TA SAMA lista co w backendzie (SpeciesGroupClassifier - eksport "sp_group"). Lista V1 do
// uzgodnienia z arborysta (np. milorzab traktujemy jak lisciaste). Modul bez Reacta - testowany przez node --test.

export type SpeciesGroup = 'CONIFER' | 'DECIDUOUS';

const CONIFER_GENERA = new Set(['abies', 'picea', 'pinus', 'larix', 'pseudotsuga', 'tsuga', 'cedrus', 'taxus', 'juniperus', 'thuja',
  'thujopsis', 'chamaecyparis', 'cupressus', 'cupressocyparis', 'cuprocyparis', 'calocedrus', 'platycladus', 'sequoia',
  'sequoiadendron', 'metasequoia', 'taxodium', 'cryptomeria', 'cunninghamia', 'sciadopitys', 'araucaria', 'microbiota', 'torreya',
  'cephalotaxus', 'podocarpus']);

const CONIFER_POLISH_NAMES = new Set(['jodła', 'świerk', 'sosna', 'modrzew', 'daglezja', 'choina', 'cedr', 'cis', 'jałowiec',
  'żywotnik', 'tuja', 'żywotnikowiec', 'cyprysik', 'cyprys', 'cyprysowiec', 'cypryśnik', 'cedrzyniec', 'biota', 'mamutowiec',
  'sekwoja', 'metasekwoja', 'kryptomeria', 'araukaria', 'torreja', 'głowocis', 'sośnica', 'zastrzalin']);

const firstWord = (text: string | null | undefined) =>
  (text ?? '').trim().toLowerCase().replace(/^[×x]\s*/, '').split(/\s+/)[0] ?? '';

/** CONIFER, DECIDUOUS albo null (gatunek nie podany). */
export function speciesGroupOf(speciesLatin: string | null | undefined, speciesPolish: string | null | undefined): SpeciesGroup | null {
  const genus = firstWord(speciesLatin);
  const polishName = firstWord(speciesPolish);
  if (!genus && !polishName) return null;
  return CONIFER_GENERA.has(genus) || CONIFER_POLISH_NAMES.has(polishName) ? 'CONIFER' : 'DECIDUOUS';
}

export const SPECIES_GROUP_LABELS: Record<SpeciesGroup, string> = { CONIFER: 'Iglaste', DECIDUOUS: 'Liściaste' };

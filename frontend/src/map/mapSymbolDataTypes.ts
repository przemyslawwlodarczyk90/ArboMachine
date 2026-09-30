// Typy katalogu symboli mapowych - odpowiadaja MapSymbolResponse (pakiet domain.mapsymbol.dto).

export type MapSymbolShape = 'CIRCLE' | 'SQUARE' | 'TRIANGLE' | 'CROSS' | 'DIAMOND' | 'X';

/**
 * Techniczny symbol V1 (ksztalt + kolor) - bez znaczenia arborystycznego. Kod (np. RED_CROSS) jest stabilny
 * i zapisywany w Tree/TreeWork.mapSymbolCode; displayName to polski opis wygladu do UI.
 */
export interface MapSymbol {
  code: string;
  displayName: string;
  shape: MapSymbolShape;
  fillColor: string;
  strokeColor: string;
}

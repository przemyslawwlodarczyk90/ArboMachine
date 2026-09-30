import type { MapSymbol } from './mapSymbolDataTypes';
import { MapSymbolSwatch } from './MapSymbolSwatch';

/**
 * Wybor symbolu z katalogu: kazda opcja to miniatura (ksztalt + kolor) i polska nazwa. Komponent tylko zglasza wybor
 * (onSelect) - zapis robi wywolujacy. allowClear dodaje opcje "Brak symbolu" (null).
 */
export function MapSymbolPicker({ symbols, selectedSymbolCode, onSelect, allowClear = false, disabled = false, groupLabel }: {
  symbols: MapSymbol[];
  selectedSymbolCode: string | null;
  onSelect: (symbolCode: string | null) => void;
  allowClear?: boolean;
  disabled?: boolean;
  groupLabel: string;
}) {
  return (
    <div className="map-symbol-picker" role="radiogroup" aria-label={groupLabel}>
      {allowClear && (
        <button type="button" role="radio" aria-checked={selectedSymbolCode === null} disabled={disabled}
                className={`map-symbol-option${selectedSymbolCode === null ? ' selected' : ''}`}
                onClick={() => onSelect(null)}>
          <span className="map-symbol-none" aria-hidden="true" /> Brak symbolu
        </button>
      )}
      {symbols.map((symbol) => (
        <button key={symbol.code} type="button" role="radio" aria-checked={selectedSymbolCode === symbol.code}
                disabled={disabled}
                className={`map-symbol-option${selectedSymbolCode === symbol.code ? ' selected' : ''}`}
                onClick={() => onSelect(symbol.code)}>
          <MapSymbolSwatch symbol={symbol} /> {symbol.displayName}
        </button>
      ))}
    </div>
  );
}

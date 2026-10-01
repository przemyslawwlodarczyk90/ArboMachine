import { valuationColorOf } from './valuationColors';
import { valuationCategoryLabel, type TreeValuationCategory } from './valuationDataTypes';

/** Odznaka kategorii waloryzacji V1 (kolor + polska nazwa - kolor nigdy nie jest jedyna informacja). */
export function ValuationBadge({ category, prefix }: { category: TreeValuationCategory | null | undefined; prefix?: string }) {
  const color = valuationColorOf(category);
  return (
    <span className="badge valuation-badge" style={{ background: color.fill, color: color.text }}>
      {prefix}{valuationCategoryLabel(category)}
    </span>
  );
}

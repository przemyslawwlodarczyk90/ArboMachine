/** Logo (stylizowane drzewo) - kolory z tokenow CSS, wiec dziala w trybie jasnym i ciemnym. */
export function ArboMachineLogo({ sizeInPixels = 28 }: { sizeInPixels?: number }) {
  return (
    <svg width={sizeInPixels} height={sizeInPixels} viewBox="0 0 32 32" aria-hidden="true">
      <rect width="32" height="32" rx="7" fill="var(--brand)" />
      <path d="M16 5 8 17h5l-4 7h14l-4-7h5z" fill="var(--brand-contrast)" />
      <rect x="14.5" y="24" width="3" height="4" fill="var(--brand-contrast)" />
    </svg>
  );
}

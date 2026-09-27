/**
 * Tymczasowa tresc sekcji workspace, ktorej modul jeszcze nie istnieje. Gdy modul powstanie,
 * jego komponent zastapi ten placeholder w trasie - bez zmian w ProjectPage.
 */
export function ProjectWorkspaceSectionPlaceholder({ sectionTitle, plannedModuleMessage }: {
  sectionTitle: string;
  plannedModuleMessage: string;
}) {
  return (
    <>
      <h2>{sectionTitle}</h2>
      <p className="muted">{plannedModuleMessage}</p>
    </>
  );
}

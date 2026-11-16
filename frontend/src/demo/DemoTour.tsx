import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { projectsApi } from '../pages/projects/projectsApi';
import { treesApi } from '../pages/trees/treesApi';
import { demoTour, useDemoTourState } from './demoTourStore';

/** Identyfikatory projektow pokazowych potrzebne do adresow krokow (pobierane raz, przy starcie samouczka). */
interface TourContext {
  parkProjectId: number;
  photoProjectId: number;
  photoTreeId: number | null;
}

interface TourStep {
  title: string;
  text: string;
  /** adres, na ktory samouczek przechodzi przed pokazaniem kroku (null = zostaje na biezacej stronie) */
  path: (context: TourContext) => string | null;
  /** element podswietlany w kroku: [data-tour="..."] */
  target: string;
}

const TOUR_STEPS: TourStep[] = [
  {
    title: 'Projekty demonstracyjne',
    text: 'Cztery zlecenia z różnymi scenariuszami: duża inwentaryzacja parku, ocena stanu alei, dokumentacja fotograficzna cmentarza i plan prac przy szkole.',
    path: () => '/projects', target: 'projects-list',
  },
  {
    title: 'Podsumowanie projektu',
    text: 'Każdy projekt zaczyna się od podsumowania: liczby drzew, zdjęć, prac i kontroli. Zakładki prowadzą do drzew, mapy, prac i dokumentów.',
    path: ({ parkProjectId }) => `/projects/${parkProjectId}`, target: 'project-summary',
  },
  {
    title: 'Mapa z przykładowymi drzewami',
    text: 'Ponad sto drzew parku na mapie. Kliknij dowolny znaczek – zobaczysz skrót danych drzewa i link do jego karty.',
    path: ({ parkProjectId }) => `/projects/${parkProjectId}/map`, target: 'project-map',
  },
  {
    title: 'Warstwy i oznaczenia',
    text: 'Przełączaj tryby mapy: symbole prac, oznaczenia, waloryzacja, gospodarka. Gwiazdka to drzewo iglaste, kółko – liściaste. Grupy drzew to osobna warstwa.',
    path: ({ parkProjectId }) => `/projects/${parkProjectId}/map`, target: 'map-modes',
  },
  {
    title: 'Karta drzewa',
    text: 'Wszystko o jednym drzewie: pomiary, gatunek, opis stanu, lokalizacja GPS, oznaczenia, waloryzacja i prace.',
    path: ({ photoProjectId, photoTreeId }) => photoTreeId ? `/projects/${photoProjectId}/trees/${photoTreeId}` : `/projects/${photoProjectId}/trees`,
    target: 'tree-data',
  },
  {
    title: 'Stan drzewa',
    text: 'Historia kontroli z oceną stanu i terminem następnej kontroli. Ocena „Zły” wysyła powiadomienie.',
    path: () => null, target: 'tree-monitoring',
  },
  {
    title: 'Zdjęcia i dokumentacja',
    text: 'Zdjęcia z adnotacjami wskazują ubytki i posusz. Przy każdym zdjęciu ustawia się podpis i to, czy trafia do dokumentu PDF.',
    path: () => null, target: 'tree-photos',
  },
  {
    title: 'Tryb terenowy',
    text: 'Ten przycisk przełącza na widok pod tablet: mapa na cały ekran, bieżąca pozycja GPS i duże przyciski. Możesz go wypróbować.',
    path: () => null, target: 'work-mode-switch',
  },
  {
    title: 'Raporty i eksport',
    text: 'Dokument projektu w PDF jednym kliknięciem. W zakładce „Import i eksport” pobierzesz CSV, XLSX, GeoJSON, Shapefile i pakiet dla QGIS.',
    path: ({ parkProjectId }) => `/projects/${parkProjectId}/documentation`, target: 'project-report',
  },
];

const HIGHLIGHT_CLASS = 'demo-tour-highlight';
const TARGET_WAIT_MS = 4000;

async function loadTourContext(): Promise<TourContext> {
  const projects = await projectsApi.listProjectsOfCurrentUser();
  const park = projects.find((project) => project.name.startsWith('Park')) ?? projects[0];
  const photoProject = projects.find((project) => project.name.startsWith('Cmentarz')) ?? park;
  const photoTrees = photoProject ? await treesApi.listTreesOfProject(photoProject.id) : [];
  return { parkProjectId: park?.id ?? 0, photoProjectId: photoProject?.id ?? 0, photoTreeId: photoTrees[0]?.id ?? null };
}

/**
 * Samouczek "Poznaj ArboMachine": przechodzi po najwazniejszych ekranach i podswietla element. Karta kroku jest
 * mala, w rogu ekranu, bez tla - reszta aplikacji dziala normalnie. Dalej / Wstecz / Pomiń / Zakończ.
 */
export function DemoTour() {
  const { isActive, stepIndex } = useDemoTourState();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const [context, setContext] = useState<TourContext | null>(null);
  const [isTargetMissing, setIsTargetMissing] = useState(false);
  const highlightedRef = useRef<HTMLElement | null>(null);
  const step = TOUR_STEPS[stepIndex];

  useEffect(() => {
    if (!isActive || context) return;
    let isStillWanted = true;
    loadTourContext()
      .then((loadedContext) => { if (isStillWanted) setContext(loadedContext); })
      .catch(() => { if (isStillWanted) setContext({ parkProjectId: 0, photoProjectId: 0, photoTreeId: null }); });
    return () => { isStillWanted = false; };
  }, [isActive, context]);

  // Przejscie na adres kroku.
  useEffect(() => {
    if (!isActive || !context || !step) return;
    const stepPath = step.path(context);
    if (stepPath && stepPath !== pathname) navigate(stepPath);
    // pathname celowo poza zaleznosciami: krok nawiguje raz, potem uzytkownik moze sam klikac dalej
  }, [isActive, context, stepIndex]);

  // Podswietlenie elementu kroku (czeka chwile, az ekran sie wczyta).
  useEffect(() => {
    const clearHighlight = () => {
      highlightedRef.current?.classList.remove(HIGHLIGHT_CLASS);
      highlightedRef.current = null;
    };
    if (!isActive || !context || !step) {
      clearHighlight();
      return;
    }
    setIsTargetMissing(false);
    const startedAt = Date.now();
    const intervalId = window.setInterval(() => {
      const target = document.querySelector<HTMLElement>(`[data-tour="${step.target}"]`);
      if (target) {
        window.clearInterval(intervalId);
        clearHighlight();
        target.classList.add(HIGHLIGHT_CLASS);
        highlightedRef.current = target;
        target.scrollIntoView({ behavior: 'smooth', block: 'center' });
      } else if (Date.now() - startedAt > TARGET_WAIT_MS) {
        window.clearInterval(intervalId);
        setIsTargetMissing(true);
      }
    }, 150);
    return () => {
      window.clearInterval(intervalId);
      clearHighlight();
    };
  }, [isActive, context, step, pathname]);

  if (!isActive || !step) return null;
  const isLastStep = stepIndex === TOUR_STEPS.length - 1;

  return (
    <aside className="demo-floating-card demo-tour-card" role="dialog" aria-labelledby="demo-tour-title" aria-live="polite">
      <span className="muted small">Poznaj ArboMachine · krok {stepIndex + 1} z {TOUR_STEPS.length}</span>
      <h2 id="demo-tour-title">{step.title}</h2>
      <p>{step.text}</p>
      {!context && <p className="muted small">Wczytywanie projektów pokazowych…</p>}
      {isTargetMissing && <p className="muted small">Tego elementu nie widać na bieżącym ekranie – przejdź dalej albo wróć.</p>}
      <div className="demo-floating-card-actions">
        <button type="button" className="btn btn-secondary btn-small" onClick={() => demoTour.goTo(stepIndex - 1)}
                disabled={stepIndex === 0}>Wstecz</button>
        {isLastStep
          ? <button type="button" className="btn btn-primary btn-small" onClick={demoTour.stop}>Zakończ</button>
          : <button type="button" className="btn btn-primary btn-small" onClick={() => demoTour.goTo(stepIndex + 1)}>Dalej</button>}
        {!isLastStep && <button type="button" className="btn btn-ghost btn-small" onClick={demoTour.stop}>Pomiń oprowadzanie</button>}
      </div>
    </aside>
  );
}

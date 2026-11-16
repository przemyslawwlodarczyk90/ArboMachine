import { useEffect, useId, useLayoutEffect, useRef, useState, useSyncExternalStore, type CSSProperties } from 'react';
import { createPortal } from 'react-dom';
import { useAuthentication } from '../auth/AuthenticationContext';
import { DEMO_HINTS, type DemoHintContent, type DemoHintId } from './demoHints';
import { useDemoPreferences } from './demoPreferences';

// Naraz otwarta jest najwyzej jedna wskazowka - otwarcie kolejnej zamyka poprzednia.
let openHintKey: string | null = null;
const openHintListeners = new Set<() => void>();

function setOpenHintKey(key: string | null) {
  openHintKey = key;
  openHintListeners.forEach((listener) => listener());
}

function useOpenHintKey() {
  return useSyncExternalStore(
    (listener) => { openHintListeners.add(listener); return () => { openHintListeners.delete(listener); }; },
    () => openHintKey,
  );
}

/**
 * Mala ikona (i) przy funkcji - tylko w trybie demo i tylko przy wlaczonych wskazowkach. Klikniecie pokazuje krotki
 * dymek z opisem funkcji (tresc z demoHints.ts). Dymek nie blokuje ekranu: zamyka go klikniecie obok, Esc albo
 * ponowne klikniecie ikony; na waskim ekranie wysuwa sie od dolu.
 */
export function DemoHint({ id, alwaysVisible = false }: {
  id: DemoHintId;
  /** pokazuj takze przy wylaczonych wskazowkach (np. opis samego trybu demo w pasku) */
  alwaysVisible?: boolean;
}) {
  const { isDemoSession } = useAuthentication();
  const { hintsEnabled } = useDemoPreferences();
  const instanceKey = useId();
  const currentlyOpenKey = useOpenHintKey();
  const buttonRef = useRef<HTMLButtonElement>(null);
  const hint: DemoHintContent = DEMO_HINTS[id];
  const isOpen = currentlyOpenKey === instanceKey;

  // Odmontowanie (np. przejscie na inna strone) zamyka wlasny dymek.
  useEffect(() => () => { if (openHintKey === instanceKey) setOpenHintKey(null); }, [instanceKey]);

  if (!isDemoSession || (!hintsEnabled && !alwaysVisible)) return null;

  return (
    <>
      <button ref={buttonRef} type="button" className={`demo-hint-button${isOpen ? ' is-open' : ''}`}
              aria-expanded={isOpen} aria-label={`Co to jest: ${hint.title}`} title={`Co to jest: ${hint.title}`}
              onClick={(clickEvent) => {
                clickEvent.preventDefault();
                clickEvent.stopPropagation();
                setOpenHintKey(isOpen ? null : instanceKey);
              }}>
        <span aria-hidden="true">i</span>
      </button>
      {isOpen && buttonRef.current && (
        <DemoHintPopover hint={hint} anchor={buttonRef.current} close={() => setOpenHintKey(null)} />
      )}
    </>
  );
}

const NARROW_SCREEN_MAX_WIDTH_PX = 600;
const VIEWPORT_MARGIN_PX = 12;

function DemoHintPopover({ hint, anchor, close }: { hint: DemoHintContent; anchor: HTMLElement; close: () => void }) {
  const popoverRef = useRef<HTMLDivElement>(null);
  const titleId = useId();
  const [position, setPosition] = useState<CSSProperties>({ visibility: 'hidden' });

  useLayoutEffect(() => {
    const place = () => {
      const popover = popoverRef.current;
      if (!popover) return;
      if (window.innerWidth <= NARROW_SCREEN_MAX_WIDTH_PX) {
        setPosition({ left: 16, right: 16, bottom: 16 });
        return;
      }
      const anchorRect = anchor.getBoundingClientRect();
      const { offsetWidth: width, offsetHeight: height } = popover;
      const left = Math.min(Math.max(VIEWPORT_MARGIN_PX, anchorRect.left + anchorRect.width / 2 - width / 2),
        window.innerWidth - width - VIEWPORT_MARGIN_PX);
      const below = anchorRect.bottom + 8;
      const top = below + height > window.innerHeight - VIEWPORT_MARGIN_PX
        ? Math.max(VIEWPORT_MARGIN_PX, anchorRect.top - height - 8) : below;
      setPosition({ left, top });
    };
    place();
    window.addEventListener('resize', place);
    window.addEventListener('scroll', place, true);
    return () => {
      window.removeEventListener('resize', place);
      window.removeEventListener('scroll', place, true);
    };
  }, [anchor]);

  useEffect(() => {
    const closeOnOutsidePointer = (pointerEvent: PointerEvent) => {
      const target = pointerEvent.target as Node;
      if (!popoverRef.current?.contains(target) && !anchor.contains(target)) close();
    };
    const closeOnEscape = (keyboardEvent: KeyboardEvent) => {
      if (keyboardEvent.key !== 'Escape') return;
      close();
      anchor.focus();
    };
    document.addEventListener('pointerdown', closeOnOutsidePointer);
    document.addEventListener('keydown', closeOnEscape);
    return () => {
      document.removeEventListener('pointerdown', closeOnOutsidePointer);
      document.removeEventListener('keydown', closeOnEscape);
    };
  }, [anchor, close]);

  return createPortal(
    // Klikniecia w dymku nie moga wyplynac do rodzica w drzewie Reacta (np. <Link> karty projektu).
    <div ref={popoverRef} className="demo-hint-popover" role="dialog" aria-labelledby={titleId} style={position}
         onClick={(clickEvent) => clickEvent.stopPropagation()}>
      <div className="demo-hint-popover-header">
        <strong id={titleId}>{hint.title}</strong>
        <button type="button" className="demo-hint-close" aria-label="Zamknij wskazówkę" onClick={close}>×</button>
      </div>
      <p>{hint.body}</p>
      {hint.lockedInDemo && (
        <p className="demo-hint-locked">Działa w pełnej wersji – w trybie demo zablokowane, bo zmienia dane.</p>
      )}
      {hint.inDemo && <p className="demo-hint-in-demo"><span>W demo:</span> {hint.inDemo}</p>}
      {hint.more && (
        <details className="demo-hint-more">
          <summary>Więcej</summary>
          <p>{hint.more}</p>
        </details>
      )}
    </div>,
    document.body,
  );
}

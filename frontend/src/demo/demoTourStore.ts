import { useSyncExternalStore } from 'react';

/** Stan samouczka "Poznaj ArboMachine" - wspolny dla widoku biurowego i trybu terenowego. */
export interface DemoTourState {
  isActive: boolean;
  stepIndex: number;
}

let state: DemoTourState = { isActive: false, stepIndex: 0 };
const listeners = new Set<() => void>();

function setState(nextState: DemoTourState) {
  state = nextState;
  listeners.forEach((listener) => listener());
}

export const demoTour = {
  start: () => setState({ isActive: true, stepIndex: 0 }),
  goTo: (stepIndex: number) => setState({ isActive: true, stepIndex }),
  stop: () => setState({ isActive: false, stepIndex: 0 }),
};

export function useDemoTourState(): DemoTourState {
  return useSyncExternalStore(
    (listener) => { listeners.add(listener); return () => { listeners.delete(listener); }; },
    () => state,
  );
}

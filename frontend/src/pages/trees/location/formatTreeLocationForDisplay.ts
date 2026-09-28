import type { TreeLocation } from '../treeDataTypes';

/** "52.2296756, 21.0122287" - szerokosc, dlugosc w stopniach dziesietnych, tak jak zapisal backend. */
export const formatTreeLocationForDisplay = (location: TreeLocation) => `${location.latitude}, ${location.longitude}`;

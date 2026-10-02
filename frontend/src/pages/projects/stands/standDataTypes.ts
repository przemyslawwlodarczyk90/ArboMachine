// Grupy drzew (etap 29) - typy odpowiadajace DTO z domain.treestand.

/** GeoJSON "Polygon" w WGS 84: pierscienie [dlugosc, szerokosc], pierwszy = obrys. */
export interface StandPolygonGeometry {
  type: 'Polygon';
  coordinates: number[][][];
}

export interface TreeStand {
  id: number;
  projectId: number;
  name: string;
  notes?: string | null;
  geometry: StandPolygonGeometry;
  /** przyblizone pole w m2 */
  areaSquareMeters?: number | null;
  source: 'MANUAL' | 'IMPORT';
  treeIds: number[];
  createdAt: string;
  updatedAt: string;
}

/** Body POST / PUT - komplet; assignTreesInside dopisuje drzewa lezace w obszarze. */
export interface TreeStandRequest {
  name: string;
  notes?: string;
  geometry: StandPolygonGeometry;
  treeIds: number[];
  assignTreesInside?: boolean;
}

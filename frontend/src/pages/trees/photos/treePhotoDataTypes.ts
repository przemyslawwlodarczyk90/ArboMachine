// Typy modulu zdjec drzew - odpowiadaja DTO backendu w pakiecie domain.treephoto.dto.

/**
 * TreePhotoResponse - same metadane (bez sciezki pliku). Tresc zdjecia pobiera sie osobno
 * (treePhotosApi.getTreePhotoContent), bo wymaga tokenu. Data jako napis ISO-8601 w UTC.
 */
export interface TreePhoto {
  id: number;
  treeId: number;
  originalFileName: string;
  /** typ wykryty przez backend z zawartosci pliku: image/jpeg, image/png albo image/webp */
  contentType: string;
  fileSizeBytes: number;
  /** czy zdjecie ma aktualna adnotacje (szkic); oryginal jest zawsze nienaruszony */
  hasAnnotation: boolean;
  annotationUpdatedAt?: string | null;
  createdAt: string;
}

/** ProjectPhotoResponse - zdjecie na liscie materialow do dokumentacji projektu. */
export interface ProjectPhoto {
  photoId: number;
  treeId: number;
  inventoryNumber: string;
  originalFileName: string;
  contentType: string;
  fileSizeBytes: number;
  hasAnnotation: boolean;
  annotationUpdatedAt?: string | null;
  createdAt: string;
}

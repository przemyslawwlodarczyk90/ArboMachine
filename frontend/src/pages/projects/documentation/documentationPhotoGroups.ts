import type { ProjectPhoto } from '../../trees/photos/treePhotoDataTypes';

// Grupowanie zdjec projektu po drzewie dla widoku materialow do dokumentacji. Modul bez importow wartosci -
// testowany przez node --test.

export interface DocumentationPhotoGroup {
  treeId: number;
  inventoryNumber: string;
  photos: ProjectPhoto[];
  annotatedPhotoCount: number;
}

/**
 * Zdjecia z GET /api/projects/{projectId}/photos -> grupy po drzewie. Kolejnosc drzew i zdjec jak z backendu
 * (drzewa i zdjecia w kolejnosci dodania) - nie sortujemy ponownie.
 */
export function groupProjectPhotosByTree(projectPhotos: ProjectPhoto[]): DocumentationPhotoGroup[] {
  const groupByTreeId = new Map<number, DocumentationPhotoGroup>();
  for (const projectPhoto of projectPhotos) {
    let group = groupByTreeId.get(projectPhoto.treeId);
    if (!group) {
      group = { treeId: projectPhoto.treeId, inventoryNumber: projectPhoto.inventoryNumber, photos: [], annotatedPhotoCount: 0 };
      groupByTreeId.set(projectPhoto.treeId, group);
    }
    group.photos.push(projectPhoto);
    if (projectPhoto.hasAnnotation) group.annotatedPhotoCount += 1;
  }
  return [...groupByTreeId.values()];
}

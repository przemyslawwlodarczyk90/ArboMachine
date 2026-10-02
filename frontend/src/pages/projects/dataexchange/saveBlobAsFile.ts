/** Plik z bloba - pobranie przez tymczasowy link (token JWT nie trafia do adresu URL). */
export function saveBlobAsFile(blob: Blob, fileName: string) {
  const objectUrl = URL.createObjectURL(blob);
  const downloadLink = document.createElement('a');
  downloadLink.href = objectUrl;
  downloadLink.download = fileName;
  downloadLink.click();
  setTimeout(() => URL.revokeObjectURL(objectUrl), 10_000);
}

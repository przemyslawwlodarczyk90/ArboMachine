import { useEffect, useRef, useState, type ChangeEvent, type FormEvent } from 'react';
import { ApiRequestError } from '../../../api/httpClient';
import { Alert } from '../../../components/Alert';
import { ModalDialog } from '../../../components/ModalDialog';
import type { TreePhoto } from './treePhotoDataTypes';
import { errorOfSelectedPhotoFile, formatFileSizeForDisplay } from './treePhotoFileRules';
import { offlineService } from '../../../offline/offlineService';
import { canCompressPhoto, readCompressPhotosPreference, saveCompressPhotosPreference } from './photoCompression';
import { compressPhotoInBrowser } from './photoCompressionInBrowser';

/** localStorage albo null (tryb prywatny / zablokowany magazyn). */
function browserStorageOrNull() {
  try {
    return typeof localStorage === 'undefined' ? null : localStorage;
  } catch {
    return null;
  }
}

/**
 * Etap 33: wysylka przez offlineService - bez polaczenia (albo dla drzewa utworzonego offline) zdjecie zapisuje sie
 * na urzadzeniu i trafia na serwer po polaczeniu (onTreePhotoQueued).
 * Okno dodawania zdjecia: wybor pliku z urzadzenia (na tablecie/telefonie przegladarka moze od razu zaproponowac
 * aparat - bez natywnej integracji), podglad, wyslanie. Po sukcesie oddaje zdjecie z odpowiedzi backendu
 * (onTreePhotoAdded); przy bledzie okno zostaje otwarte z wybranym plikiem.
 * Etap 40: opcjonalne zmniejszenie zdjecia przed wyslaniem (wybor zapamietany na urzadzeniu, domyslnie wylaczony).
 */
export function AddTreePhotoDialog({ projectId, treeId, onTreePhotoAdded, onTreePhotoQueued, closeDialog, initialFile = null }: {
  projectId: number;
  treeId: number;
  onTreePhotoAdded: (addedTreePhoto: TreePhoto) => void;
  /** etap 33: zdjecie zapisane na urzadzeniu (bez polaczenia) - wysle sie po polaczeniu */
  onTreePhotoQueued: () => void;
  closeDialog: () => void;
  /** etap 32 (tryb terenowy): zdjecie juz zrobione aparatem albo wybrane z galerii - okno je pokazuje i wysyla */
  initialFile?: File | null;
}) {
  const [selectedFile, setSelectedFile] = useState<File | null>(initialFile);
  const [previewObjectUrl, setPreviewObjectUrl] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(() => (initialFile ? errorOfSelectedPhotoFile(initialFile) : null));
  const [isUploading, setIsUploading] = useState(false);
  const [compressesPhoto, setCompressesPhoto] = useState(() => readCompressPhotosPreference(browserStorageOrNull()));
  const changeCompressesPhoto = (checked: boolean) => {
    setCompressesPhoto(checked);
    saveCompressPhotosPreference(browserStorageOrNull(), checked);
  };
  // Synchroniczna blokada podwojnego wyslania - stan Reacta zmienia sie dopiero po renderze.
  const isUploadInProgress = useRef(false);

  // Podglad z lokalnego pliku; adres zwalniany przy zmianie pliku i zamknieciu okna.
  useEffect(() => {
    if (!selectedFile) return;
    const objectUrl = URL.createObjectURL(selectedFile);
    setPreviewObjectUrl(objectUrl);
    return () => URL.revokeObjectURL(objectUrl);
  }, [selectedFile]);

  const selectFile = (changeEvent: ChangeEvent<HTMLInputElement>) => {
    const chosenFile = changeEvent.target.files?.[0] ?? null;
    setPreviewObjectUrl(null);
    setSelectedFile(chosenFile);
    setErrorMessage(chosenFile ? errorOfSelectedPhotoFile(chosenFile) : null);
  };

  const uploadSelectedFile = async (submitEvent: FormEvent) => {
    submitEvent.preventDefault();
    if (!selectedFile || isUploadInProgress.current) return;
    const selectedFileError = errorOfSelectedPhotoFile(selectedFile);
    if (selectedFileError) {
      setErrorMessage(selectedFileError);
      return;
    }
    isUploadInProgress.current = true;
    setIsUploading(true);
    setErrorMessage(null);
    try {
      const fileToUpload = compressesPhoto ? (await compressPhotoInBrowser(selectedFile)).file : selectedFile;
      const uploadResult = await offlineService.uploadTreePhoto<TreePhoto>(projectId, treeId, fileToUpload);
      if (uploadResult.kind === 'uploaded') onTreePhotoAdded(uploadResult.photo);
      else onTreePhotoQueued();
    } catch (uploadError) {
      setErrorMessage(uploadError instanceof ApiRequestError ? uploadError.message : 'Nieoczekiwany błąd.');
      isUploadInProgress.current = false;
      setIsUploading(false);
    }
  };

  return (
    <ModalDialog title="Dodaj zdjęcie" closeDialog={closeDialog} isClosingBlocked={isUploading}>
      <form onSubmit={uploadSelectedFile} className="form" noValidate>
        {errorMessage && <Alert variant="error">{errorMessage}</Alert>}
        <div className="field">
          <label htmlFor="tree-photo-file-input">Zdjęcie *</label>
          {/* accept to tylko podpowiedz dla przegladarki - typ i rozmiar sprawdza backend. */}
          <input id="tree-photo-file-input" type="file" accept="image/*" onChange={selectFile} disabled={isUploading} />
          <span className="field-hint">JPEG, PNG albo WebP.</span>
        </div>
        {selectedFile && (
          <div className="tree-photo-upload-preview">
            {previewObjectUrl && <img src={previewObjectUrl} alt="Podgląd wybranego zdjęcia" />}
            <span className="small">
              {selectedFile.name} <span className="muted">({formatFileSizeForDisplay(selectedFile.size)})</span>
            </span>
          </div>
        )}
        <label className="checkbox-row small">
          <input type="checkbox" checked={compressesPhoto} disabled={isUploading}
                 onChange={(changeEvent) => changeCompressesPhoto(changeEvent.target.checked)} />
          <span>Zmniejsz zdjęcie przed wysłaniem (dłuższy bok do 2560 px)</span>
        </label>
        <span className="field-hint">
          Mniej miejsca na urządzeniu bez zasięgu i szybsza wysyłka. Zmniejszone zdjęcie traci dane z aparatu (EXIF, np.
          datę i położenie). Dotyczy JPEG i WebP powyżej 1 MB - PNG i małe pliki idą bez zmian.
          {compressesPhoto && selectedFile && !canCompressPhoto(selectedFile) && ' To zdjęcie zostanie wysłane bez zmian.'}
        </span>
        <div className="form-actions">
          <button type="button" className="btn btn-ghost" onClick={closeDialog} disabled={isUploading}>Anuluj</button>
          <button type="submit" className="btn btn-primary" disabled={!selectedFile || isUploading}>
            {isUploading ? 'Dodawanie zdjęcia…' : 'Dodaj zdjęcie'}
          </button>
        </div>
      </form>
    </ModalDialog>
  );
}

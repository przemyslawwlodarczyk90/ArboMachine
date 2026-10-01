import {
  ANNOTATION_COLORS, ANNOTATION_LIMITS, ANNOTATION_STROKE_SIZES, type AnnotationColor, type AnnotationElementType,
  type AnnotationStrokeSize,
} from './annotationDataTypes';

const TOOLS: { tool: AnnotationElementType; label: string }[] = [
  { tool: 'FREEHAND', label: 'Rysuj' },
  { tool: 'LINE', label: 'Linia' },
  { tool: 'ARROW', label: 'Strzałka' },
  { tool: 'TEXT', label: 'Tekst' },
];

/**
 * Pasek narzedzi edytora adnotacji: narzedzie, kolor, grubosc, tekst (dla narzedzia "Tekst") oraz Cofnij/Ponow/Wyczysc.
 * Tylko zglasza wybory - stan trzyma TreePhotoAnnotationDialog. Przyciski wyboru maja aria-pressed.
 */
export function PhotoAnnotationToolbar({
  tool, color, strokeSize, pendingText, canUndo, canRedo, canClear, disabled,
  onToolChange, onColorChange, onStrokeSizeChange, onPendingTextChange, onUndo, onRedo, onClear,
}: {
  tool: AnnotationElementType;
  color: AnnotationColor;
  strokeSize: AnnotationStrokeSize;
  pendingText: string;
  canUndo: boolean;
  canRedo: boolean;
  canClear: boolean;
  disabled: boolean;
  onToolChange: (tool: AnnotationElementType) => void;
  onColorChange: (color: AnnotationColor) => void;
  onStrokeSizeChange: (strokeSize: AnnotationStrokeSize) => void;
  onPendingTextChange: (text: string) => void;
  onUndo: () => void;
  onRedo: () => void;
  onClear: () => void;
}) {
  return (
    <div className="photo-annotation-toolbar">
      <div className="photo-annotation-toolbar-group" role="group" aria-label="Narzędzie">
        {TOOLS.map((toolOption) => (
          <button key={toolOption.tool} type="button" disabled={disabled} aria-pressed={tool === toolOption.tool}
                  className={`btn btn-small ${tool === toolOption.tool ? 'btn-primary' : 'btn-secondary'}`}
                  onClick={() => onToolChange(toolOption.tool)}>
            {toolOption.label}
          </button>
        ))}
      </div>
      <div className="photo-annotation-toolbar-group" role="group" aria-label="Kolor">
        {ANNOTATION_COLORS.map((colorOption) => (
          <button key={colorOption.color} type="button" disabled={disabled} aria-pressed={color === colorOption.color}
                  className={`photo-annotation-color${color === colorOption.color ? ' selected' : ''}`}
                  onClick={() => onColorChange(colorOption.color)} title={colorOption.label}>
            <span className="photo-annotation-color-dot" style={{ background: colorOption.hex }} aria-hidden="true" />
            {colorOption.label}
          </button>
        ))}
      </div>
      <div className="photo-annotation-toolbar-group" role="group" aria-label="Grubość">
        {ANNOTATION_STROKE_SIZES.map((sizeOption) => (
          <button key={sizeOption.strokeSize} type="button" disabled={disabled} aria-pressed={strokeSize === sizeOption.strokeSize}
                  className={`btn btn-small ${strokeSize === sizeOption.strokeSize ? 'btn-primary' : 'btn-secondary'}`}
                  onClick={() => onStrokeSizeChange(sizeOption.strokeSize)}>
            {sizeOption.label}
          </button>
        ))}
      </div>
      {tool === 'TEXT' && (
        <div className="field photo-annotation-text-field">
          <label htmlFor="photo-annotation-text">Tekst do wstawienia</label>
          <input id="photo-annotation-text" type="text" value={pendingText} maxLength={ANNOTATION_LIMITS.maximumTextLength}
                 disabled={disabled} placeholder="np. Ciąć tutaj" autoComplete="off"
                 onChange={(changeEvent) => onPendingTextChange(changeEvent.target.value)} />
          <span className="field-hint">Wpisz tekst, a potem dotknij zdjęcia w miejscu, gdzie ma się pojawić.</span>
        </div>
      )}
      <div className="photo-annotation-toolbar-group" role="group" aria-label="Historia zmian">
        <button type="button" className="btn btn-small btn-secondary" onClick={onUndo} disabled={disabled || !canUndo}>Cofnij</button>
        <button type="button" className="btn btn-small btn-secondary" onClick={onRedo} disabled={disabled || !canRedo}>Ponów</button>
        <button type="button" className="btn btn-small btn-danger-outline" onClick={onClear} disabled={disabled || !canClear}>
          Wyczyść
        </button>
      </div>
    </div>
  );
}

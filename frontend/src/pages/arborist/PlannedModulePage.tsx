import { Link, Navigate, useParams } from 'react-router-dom';
import { ToBeClarifiedNote } from '../../components/ToBeClarifiedNote';
import { PlannedModuleIcon } from './PlannedModuleIcon';
import { plannedModuleBySlug, PLANNED_MODULES } from './plannedModules';

/** Etapy budowy modulu - wszystkie moduly sa na etapie konsultacji z arborysta. */
const STAGES = ['Pomysł', 'Konsultacja z arborystą', 'Budowa', 'Gotowe'];
const CURRENT_STAGE_INDEX = 1;

/**
 * Strona modulu panelu arborysty w budowie (/arborist/:moduleSlug): czym bedzie modul, co juz dziala w projektach,
 * co planujemy i co trzeba ustalic. Nieznany adres -> lista projektow.
 */
export function PlannedModulePage() {
  const plannedModule = plannedModuleBySlug(useParams().moduleSlug);
  if (!plannedModule) return <Navigate to="/projects" replace />;
  const otherModules = PLANNED_MODULES.filter((module) => module.slug !== plannedModule.slug);

  return (
    <div className="stack-lg planned-module-page">
      <header className="planned-module-hero card">
        <span className="planned-module-hero-icon"><PlannedModuleIcon icon={plannedModule.icon} size={32} /></span>
        <div className="planned-module-hero-text">
          <div className="planned-module-title-row">
            <h1>{plannedModule.title}</h1>
            <span className="under-construction-badge">W budowie</span>
          </div>
          <p className="muted">{plannedModule.lead}</p>
        </div>
      </header>

      <ol className="planned-module-stages" aria-label="Etap budowy modułu">
        {STAGES.map((stage, index) => (
          <li key={stage} className={index < CURRENT_STAGE_INDEX ? 'done' : index === CURRENT_STAGE_INDEX ? 'current' : ''}
              aria-current={index === CURRENT_STAGE_INDEX ? 'step' : undefined}>
            <span className="planned-module-stage-dot" aria-hidden="true">{index < CURRENT_STAGE_INDEX ? '✓' : index + 1}</span>
            <span>{stage}</span>
          </li>
        ))}
      </ol>

      <div className="planned-module-columns">
        <section className="card planned-module-card">
          <h2>Już działa w projektach</h2>
          <ul className="planned-module-list planned-module-list-available">
            {plannedModule.alreadyAvailable.map((item) => <li key={item}>{item}</li>)}
          </ul>
          <Link to="/projects" className="btn btn-secondary btn-small">Przejdź do projektów</Link>
        </section>
        <section className="card planned-module-card">
          <h2>Co planujemy</h2>
          <ul className="planned-module-list planned-module-list-planned">
            {plannedModule.planned.map((item) => <li key={item}>{item}</li>)}
          </ul>
        </section>
      </div>

      <section className="stack">
        <h2 className="section-title">Do ustalenia z arborystą</h2>
        {plannedModule.openQuestions.map((question) => <ToBeClarifiedNote key={question}>{question}</ToBeClarifiedNote>)}
      </section>

      <section>
        <h2 className="section-title">Inne moduły w budowie</h2>
        <div className="module-grid">
          {otherModules.map((module) => (
            <Link key={module.slug} to={`/arborist/${module.slug}`} className="card module-card module-card-link">
              <span className="planned-module-mini-icon"><PlannedModuleIcon icon={module.icon} size={20} /></span>
              <h3>{module.title}</h3>
              <p className="muted small">{module.lead}</p>
            </Link>
          ))}
        </div>
      </section>
    </div>
  );
}

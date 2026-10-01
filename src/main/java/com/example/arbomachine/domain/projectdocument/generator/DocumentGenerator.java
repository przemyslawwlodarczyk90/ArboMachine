package com.example.arbomachine.domain.projectdocument.generator;

import com.example.arbomachine.domain.projectdocument.data.ProjectDocumentData;

/**
 * Wymienialny punkt wejscia generatora dokumentu. V1: ProjectReportPdfGenerator (szablon HTML -> PDF).
 * Pozniej moze dojsc np. generator programu prac konserwatorskich albo DOCX - bez zmian w assemblerze i API.
 */
public interface DocumentGenerator {
    GeneratedDocument generate(ProjectDocumentData data);
}

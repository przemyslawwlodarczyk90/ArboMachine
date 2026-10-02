package com.example.arbomachine.domain.treemonitoring.service.treemonitoringfacade;

import com.example.arbomachine.domain.treemonitoring.dto.TreeMonitoringRecordRequest;
import com.example.arbomachine.domain.treemonitoring.dto.TreeMonitoringRecordResponse;
import com.example.arbomachine.domain.treemonitoring.entity.TreeHealthStatus;
import com.example.arbomachine.domain.treemonitoring.entity.TreeMonitoringRecord;

/** Request -> znormalizowane pola (uwagi przyciete, puste = null) i encja -> odpowiedz API. Pomocnik handlerow fasady. */
final class TreeMonitoringRecordMapper {

    private TreeMonitoringRecordMapper() {
    }

    static TreeMonitoringRecord.Fields fieldsOf(TreeMonitoringRecordRequest request) {
        String notes = request.notes();
        return new TreeMonitoringRecord.Fields(
                request.inspectionDate(),
                request.healthStatus() == null ? null : TreeHealthStatus.valueOf(request.healthStatus()),
                notes == null || notes.isBlank() ? null : notes.trim());
    }

    static TreeMonitoringRecordResponse toResponse(TreeMonitoringRecord record) {
        return new TreeMonitoringRecordResponse(record.getId(), record.getTree().getId(), record.getInspectionDate(),
                record.getHealthStatus() == null ? null : record.getHealthStatus().name(), record.getNotes(),
                record.getCreatedAt(), record.getUpdatedAt());
    }
}

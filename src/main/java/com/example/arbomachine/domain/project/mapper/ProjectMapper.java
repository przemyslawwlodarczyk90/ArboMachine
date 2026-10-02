package com.example.arbomachine.domain.project.mapper;

import com.example.arbomachine.domain.project.dto.ProjectResponse;
import com.example.arbomachine.domain.project.entity.Project;
import org.springframework.stereotype.Component;

/** Encja Project -> ProjectResponse. Nie dotyka wlasciciela (leniwie ladowana relacja). */
@Component
public class ProjectMapper {

    public ProjectResponse toProjectResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getClientName(),
                project.getSiteAddress(),
                project.getOrderDate(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}

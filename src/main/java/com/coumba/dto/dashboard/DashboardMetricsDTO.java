package com.coumba.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardMetricsDTO {

    private LocalDateTime generatedAt;

    // Métriques globales de tâches
    private long totalTasks;
    private long openTasks;          // A_FAIRE + EN_COURS
    private long inProgressTasks;     // EN_COURS
    private long completedTasks;      // TERMINE
    private long closedTickets;       // CLOTURE
    private long totalFinishedTasks;  // TERMINE + CLOTURE
    private long overdueTasksCount;   // Échéance dépassée & non terminé/clôturé
    private double completionRate;    // Taux de complétion en % (0.0 à 100.0)

    // Répartition
    private Map<String, Long> tasksByStatus;
    private Map<String, Long> tasksByPriority;

    // Projets & Utilisateurs
    private long totalProjects;
    private long activeProjects;
    private long totalUsers;

    // Charge de travail par collaborateur
    private List<CollaboratorWorkloadDTO> collaboratorWorkloads;
}

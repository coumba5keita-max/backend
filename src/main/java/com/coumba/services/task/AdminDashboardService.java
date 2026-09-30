package com.coumba.services.task;

import com.coumba.dto.dashboard.CollaboratorWorkloadDTO;
import com.coumba.dto.dashboard.DashboardMetricsDTO;
import com.coumba.entities.enums.TaskPriority;
import com.coumba.entities.enums.TaskStatus;
import com.coumba.entities.project.Project;
import com.coumba.entities.task.Task;
import com.coumba.entities.user.User;
import com.coumba.repositories.project.ProjectRepository;
import com.coumba.repositories.task.TaskRepository;
import com.coumba.repositories.user.UserRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Calcule l'ensemble des métriques globales et la charge de travail par collaborateur.
     */
    @Transactional(readOnly = true)
    public DashboardMetricsDTO getMetrics() {
        List<Task> allTasks = taskRepository.findAll();
        List<User> allUsers = userRepository.findAll();
        List<Project> allProjects = projectRepository.findAll();
        LocalDate today = LocalDate.now();

        long totalTasks = allTasks.size();
        long openTasks = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.A_FAIRE || t.getStatus() == TaskStatus.EN_COURS).count();
        long inProgressTasks = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.EN_COURS).count();
        long completedTasks = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.TERMINE).count();
        long closedTickets = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.CLOTURE).count();
        long totalFinished = completedTasks + closedTickets;

        long overdueTasksCount = allTasks.stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(today)
                        && t.getStatus() != TaskStatus.TERMINE && t.getStatus() != TaskStatus.CLOTURE)
                .count();

        double completionRate = totalTasks > 0
                ? Math.round(((double) totalFinished / totalTasks) * 1000.0) / 10.0
                : 0.0;

        // Répartition par statut
        Map<String, Long> tasksByStatus = new LinkedHashMap<>();
        for (TaskStatus s : TaskStatus.values()) {
            long count = allTasks.stream().filter(t -> t.getStatus() == s).count();
            tasksByStatus.put(s.name(), count);
        }

        // Répartition par priorité
        Map<String, Long> tasksByPriority = new LinkedHashMap<>();
        for (TaskPriority p : TaskPriority.values()) {
            long count = allTasks.stream().filter(t -> t.getPriority() == p).count();
            tasksByPriority.put(p.name(), count);
        }

        // Projets
        long totalProjects = allProjects.size();
        long activeProjects = allProjects.stream().filter(p -> p.getIsArchived() == null || !p.getIsArchived()).count();

        // Charge de travail par collaborateur
        long totalActiveTasks = openTasks;
        List<CollaboratorWorkloadDTO> workloads = new ArrayList<>();

        for (User user : allUsers) {
            List<Task> userTasks = allTasks.stream()
                    .filter(t -> t.getAssignees() != null && t.getAssignees().stream().anyMatch(a -> a.getId().equals(user.getId())))
                    .toList();

            long assignedCount = userTasks.size();
            long inProgressCount = userTasks.stream().filter(t -> t.getStatus() == TaskStatus.EN_COURS).count();
            long completedCount = userTasks.stream().filter(t -> t.getStatus() == TaskStatus.TERMINE || t.getStatus() == TaskStatus.CLOTURE).count();
            long overdueCount = userTasks.stream()
                    .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(today)
                            && t.getStatus() != TaskStatus.TERMINE && t.getStatus() != TaskStatus.CLOTURE)
                    .count();

            long activeUserTasks = userTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.A_FAIRE || t.getStatus() == TaskStatus.EN_COURS)
                    .count();

            double workloadPercent = totalActiveTasks > 0
                    ? Math.round(((double) activeUserTasks / totalActiveTasks) * 1000.0) / 10.0
                    : 0.0;

            workloads.add(CollaboratorWorkloadDTO.builder()
                    .userId(user.getId())
                    .fullName(user.getFirstname() + " " + user.getLastname())
                    .email(user.getEmail())
                    .role(user.getRole() != null ? user.getRole().name() : "ROLE_USER")
                    .assignedTasksCount(assignedCount)
                    .inProgressTasksCount(inProgressCount)
                    .overdueTasksCount(overdueCount)
                    .completedTasksCount(completedCount)
                    .workloadPercentage(workloadPercent)
                    .build());
        }

        // Trier les collaborateurs par nombre de tâches assignées décroissant
        workloads.sort(Comparator.comparingLong(CollaboratorWorkloadDTO::getAssignedTasksCount).reversed());

        return DashboardMetricsDTO.builder()
                .generatedAt(LocalDateTime.now())
                .totalTasks(totalTasks)
                .openTasks(openTasks)
                .inProgressTasks(inProgressTasks)
                .completedTasks(completedTasks)
                .closedTickets(closedTickets)
                .totalFinishedTasks(totalFinished)
                .overdueTasksCount(overdueTasksCount)
                .completionRate(completionRate)
                .tasksByStatus(tasksByStatus)
                .tasksByPriority(tasksByPriority)
                .totalProjects(totalProjects)
                .activeProjects(activeProjects)
                .totalUsers(allUsers.size())
                .collaboratorWorkloads(workloads)
                .build();
    }

    /**
     * Génère un export de rapport d'activité au format CSV (avec UTF-8 BOM pour Excel).
     */
    @Transactional(readOnly = true)
    public byte[] exportCsv(Long projectId, TaskStatus status) {
        log.info("Génération de l'export CSV (Filtre Projet: {}, Statut: {})", projectId, status);

        List<Task> tasks = taskRepository.findAll().stream()
                .filter(t -> projectId == null || (t.getProject() != null && t.getProject().getId().equals(projectId)))
                .filter(t -> status == null || t.getStatus() == status)
                .sorted(Comparator.comparing(Task::getId))
                .toList();

        LocalDate today = LocalDate.now();
        StringBuilder csv = new StringBuilder();

        // Ajout du BOM UTF-8 (\uFEFF) pour compatibilité totale avec Excel sous Windows
        csv.append('\uFEFF');

        // En-têtes CSV
        csv.append("ID;Titre;Description;Projet;Catégorie;Priorité;Statut;Assigné(s);Date Échéance;En Retard;Date Création;Date Clôture\n");

        for (Task t : tasks) {
            String id = String.valueOf(t.getId());
            String title = escapeCsv(t.getTitle());
            String description = escapeCsv(t.getDescription());
            String projectName = t.getProject() != null ? escapeCsv(t.getProject().getName()) : "N/A";
            String categoryName = t.getCategory() != null ? escapeCsv(t.getCategory().getName()) : "Général";
            String priority = t.getPriority() != null ? t.getPriority().name() : "MOYENNE";
            String taskStatus = t.getStatus() != null ? t.getStatus().name() : "A_FAIRE";

            String assignees = t.getAssignees() != null && !t.getAssignees().isEmpty()
                    ? escapeCsv(t.getAssignees().stream().map(u -> u.getFirstname() + " " + u.getLastname()).collect(Collectors.joining(", ")))
                    : "Non assigné";

            String dueDate = t.getDueDate() != null ? t.getDueDate().format(DATE_FORMATTER) : "Non définie";

            boolean isOverdue = t.getDueDate() != null && t.getDueDate().isBefore(today)
                    && t.getStatus() != TaskStatus.TERMINE && t.getStatus() != TaskStatus.CLOTURE;
            String overdueStr = isOverdue ? "OUI" : "NON";

            String createdAt = t.getCreatedAt() != null ? t.getCreatedAt().format(DATE_TIME_FORMATTER) : "";
            String closedAt = t.getClosedAt() != null ? t.getClosedAt().format(DATE_TIME_FORMATTER) : "";

            csv.append(String.join(";",
                    id, title, description, projectName, categoryName,
                    priority, taskStatus, assignees, dueDate, overdueStr, createdAt, closedAt
            )).append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Génère un rapport d'activité complet et élégant au format PDF via OpenPDF.
     */
    @Transactional(readOnly = true)
    public byte[] exportPdf(Long projectId, TaskStatus status, String adminEmail) {
        log.info("Génération du rapport PDF d'activité pour l'admin [{}]", adminEmail);

        DashboardMetricsDTO metrics = getMetrics();
        List<Task> tasks = taskRepository.findAll().stream()
                .filter(t -> projectId == null || (t.getProject() != null && t.getProject().getId().equals(projectId)))
                .filter(t -> status == null || t.getStatus() == status)
                .sorted(Comparator.comparing(Task::getId))
                .toList();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Polices
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(26, 54, 93));
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(74, 85, 104));
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(43, 108, 176));
            Font thFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font tdFont = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(45, 55, 72));
            Font tdBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(45, 55, 72));
            Font alertFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new Color(229, 62, 62));

            // Couleurs de design
            Color headerBg = new Color(43, 108, 176);
            Color altRowBg = new Color(247, 250, 252);

            // En-tête du document
            Paragraph title = new Paragraph("TaskManager - Rapport d'Activité & Métriques Globales", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4);
            document.add(title);

            Paragraph meta = new Paragraph("Généré le " + LocalDateTime.now().format(DATE_TIME_FORMATTER) + " par l'administrateur : " + adminEmail, subTitleFont);
            meta.setAlignment(Element.ALIGN_CENTER);
            meta.setSpacingAfter(15);
            document.add(meta);

            // --- SECTION 1 : RÉCAPITULATIF DES MÉTRIQUES CLÉS ---
            Paragraph sec1 = new Paragraph("1. Synthèse Globale des Indicateurs Clés", sectionFont);
            sec1.setSpacingAfter(6);
            document.add(sec1);

            PdfPTable metricsTable = new PdfPTable(6);
            metricsTable.setWidthPercentage(100);
            metricsTable.setWidths(new float[]{16f, 16f, 16f, 16f, 18f, 18f});
            metricsTable.setSpacingAfter(15);

            addThCell(metricsTable, "Total Tâches", headerBg, thFont);
            addThCell(metricsTable, "En Cours", headerBg, thFont);
            addThCell(metricsTable, "Clôturées/Terminées", headerBg, thFont);
            addThCell(metricsTable, "En Retard", headerBg, thFont);
            addThCell(metricsTable, "Taux de Complétion", headerBg, thFont);
            addThCell(metricsTable, "Projets Actifs", headerBg, thFont);

            addTdCell(metricsTable, String.valueOf(metrics.getTotalTasks()), tdBold, Element.ALIGN_CENTER, Color.WHITE);
            addTdCell(metricsTable, String.valueOf(metrics.getInProgressTasks()), tdFont, Element.ALIGN_CENTER, Color.WHITE);
            addTdCell(metricsTable, String.valueOf(metrics.getTotalFinishedTasks()), tdFont, Element.ALIGN_CENTER, Color.WHITE);
            addTdCell(metricsTable, String.valueOf(metrics.getOverdueTasksCount()), metrics.getOverdueTasksCount() > 0 ? alertFont : tdFont, Element.ALIGN_CENTER, Color.WHITE);
            addTdCell(metricsTable, metrics.getCompletionRate() + " %", tdBold, Element.ALIGN_CENTER, Color.WHITE);
            addTdCell(metricsTable, metrics.getActiveProjects() + " / " + metrics.getTotalProjects(), tdFont, Element.ALIGN_CENTER, Color.WHITE);

            document.add(metricsTable);

            // --- SECTION 2 : CHARGE DE TRAVAIL PAR COLLABORATEUR ---
            Paragraph sec2 = new Paragraph("2. Répartition de la Charge de Travail par Collaborateur", sectionFont);
            sec2.setSpacingAfter(6);
            document.add(sec2);

            PdfPTable workloadTable = new PdfPTable(7);
            workloadTable.setWidthPercentage(100);
            workloadTable.setWidths(new float[]{20f, 25f, 12f, 11f, 11f, 11f, 10f});
            workloadTable.setSpacingAfter(15);

            addThCell(workloadTable, "Collaborateur", headerBg, thFont);
            addThCell(workloadTable, "Email", headerBg, thFont);
            addThCell(workloadTable, "Total Assignées", headerBg, thFont);
            addThCell(workloadTable, "En Cours", headerBg, thFont);
            addThCell(workloadTable, "En Retard", headerBg, thFont);
            addThCell(workloadTable, "Terminées", headerBg, thFont);
            addThCell(workloadTable, "Part Charge", headerBg, thFont);

            boolean alt = false;
            for (CollaboratorWorkloadDTO w : metrics.getCollaboratorWorkloads()) {
                Color rowBg = alt ? altRowBg : Color.WHITE;
                addTdCell(workloadTable, w.getFullName(), tdBold, Element.ALIGN_LEFT, rowBg);
                addTdCell(workloadTable, w.getEmail(), tdFont, Element.ALIGN_LEFT, rowBg);
                addTdCell(workloadTable, String.valueOf(w.getAssignedTasksCount()), tdFont, Element.ALIGN_CENTER, rowBg);
                addTdCell(workloadTable, String.valueOf(w.getInProgressTasksCount()), tdFont, Element.ALIGN_CENTER, rowBg);
                addTdCell(workloadTable, String.valueOf(w.getOverdueTasksCount()), w.getOverdueTasksCount() > 0 ? alertFont : tdFont, Element.ALIGN_CENTER, rowBg);
                addTdCell(workloadTable, String.valueOf(w.getCompletedTasksCount()), tdFont, Element.ALIGN_CENTER, rowBg);
                addTdCell(workloadTable, w.getWorkloadPercentage() + " %", tdBold, Element.ALIGN_CENTER, rowBg);
                alt = !alt;
            }

            document.add(workloadTable);

            // --- SECTION 3 : ÉTAT DÉTAILLÉ DES TÂCHES & TICKETS ---
            Paragraph sec3 = new Paragraph("3. Registre Détaillé des Tâches et Tickets (" + tasks.size() + " résultats)", sectionFont);
            sec3.setSpacingAfter(6);
            document.add(sec3);

            PdfPTable tasksTable = new PdfPTable(8);
            tasksTable.setWidthPercentage(100);
            tasksTable.setWidths(new float[]{6f, 26f, 14f, 10f, 11f, 11f, 14f, 8f});

            addThCell(tasksTable, "ID", headerBg, thFont);
            addThCell(tasksTable, "Titre", headerBg, thFont);
            addThCell(tasksTable, "Projet", headerBg, thFont);
            addThCell(tasksTable, "Priorité", headerBg, thFont);
            addThCell(tasksTable, "Statut", headerBg, thFont);
            addThCell(tasksTable, "Échéance", headerBg, thFont);
            addThCell(tasksTable, "Assigné(s)", headerBg, thFont);
            addThCell(tasksTable, "Retard", headerBg, thFont);

            LocalDate today = LocalDate.now();
            alt = false;
            for (Task t : tasks) {
                Color rowBg = alt ? altRowBg : Color.WHITE;
                boolean isOverdue = t.getDueDate() != null && t.getDueDate().isBefore(today)
                        && t.getStatus() != TaskStatus.TERMINE && t.getStatus() != TaskStatus.CLOTURE;

                String assignees = t.getAssignees() != null && !t.getAssignees().isEmpty()
                        ? t.getAssignees().stream().map(u -> u.getFirstname() + " " + u.getLastname()).collect(Collectors.joining(", "))
                        : "Non assigné";

                addTdCell(tasksTable, String.valueOf(t.getId()), tdFont, Element.ALIGN_CENTER, rowBg);
                addTdCell(tasksTable, t.getTitle(), tdBold, Element.ALIGN_LEFT, rowBg);
                addTdCell(tasksTable, t.getProject() != null ? t.getProject().getName() : "-", tdFont, Element.ALIGN_LEFT, rowBg);
                addTdCell(tasksTable, t.getPriority() != null ? t.getPriority().name() : "-", tdFont, Element.ALIGN_CENTER, rowBg);
                addTdCell(tasksTable, t.getStatus() != null ? t.getStatus().name() : "-", tdFont, Element.ALIGN_CENTER, rowBg);
                addTdCell(tasksTable, t.getDueDate() != null ? t.getDueDate().format(DATE_FORMATTER) : "-", tdFont, Element.ALIGN_CENTER, rowBg);
                addTdCell(tasksTable, assignees, tdFont, Element.ALIGN_LEFT, rowBg);
                addTdCell(tasksTable, isOverdue ? "OUI" : "NON", isOverdue ? alertFont : tdFont, Element.ALIGN_CENTER, rowBg);

                alt = !alt;
            }

            document.add(tasksTable);
            document.close();

            log.info("Rapport PDF généré avec succès (Taille : {} octets)", baos.size());
            return baos.toByteArray();

        } catch (DocumentException e) {
            log.error("Erreur lors de la génération du document PDF : {}", e.getMessage());
            throw new RuntimeException("Erreur de génération PDF : " + e.getMessage(), e);
        }
    }

    private void addThCell(PdfPTable table, String text, Color bgColor, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private void addTdCell(PdfPTable table, String text, Font font, int alignment, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4f);
        table.addCell(cell);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"").replace("\n", " ").replace("\r", "");
        if (escaped.contains(";") || escaped.contains("\"") || escaped.contains(",")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}

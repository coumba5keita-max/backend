package com.coumba.controllers.task;

import com.coumba.dto.dashboard.DashboardMetricsDTO;
import com.coumba.entities.enums.TaskStatus;
import com.coumba.services.task.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    /**
     * GET /api/admin/dashboard/metrics : Consulter les métriques globales
     * (tâches clôturées, retards, charge de travail par collaborateur, etc.).
     */
    @GetMapping("/dashboard/metrics")
    public ResponseEntity<DashboardMetricsDTO> getDashboardMetrics() {
        return ResponseEntity.ok(adminDashboardService.getMetrics());
    }

    /**
     * GET /api/admin/reports/export/csv : Exporter le rapport d'activité au format CSV (compatible Excel).
     */
    @GetMapping("/reports/export/csv")
    public ResponseEntity<byte[]> exportCsvReport(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) TaskStatus status
    ) {
        byte[] csvData = adminDashboardService.exportCsv(projectId, status);
        String filename = "rapport_activite_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    /**
     * GET /api/admin/reports/export/pdf : Exporter le rapport d'activité au format PDF élégant.
     */
    @GetMapping("/reports/export/pdf")
    public ResponseEntity<byte[]> exportPdfReport(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) TaskStatus status,
            Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : "ADMIN";
        byte[] pdfData = adminDashboardService.exportPdf(projectId, status, adminEmail);
        String filename = "rapport_activite_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }
}

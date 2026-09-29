package com.coumba.controllers.task;

import com.coumba.dto.task.CategoryRequest;
import com.coumba.dto.task.CategoryResponseDTO;
import com.coumba.dto.task.PriorityResponseDTO;
import com.coumba.services.task.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;

    // --- Endpoints Publics / Utilisateurs Authentifiés ---

    /**
     * GET /api/categories : Récupère la liste de toutes les catégories.
     */
    @GetMapping("/api/categories")
    public ResponseEntity<List<CategoryResponseDTO>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    /**
     * GET /api/categories/{id} : Récupère les détails d'une catégorie par ID.
     */
    @GetMapping("/api/categories/{id}")
    public ResponseEntity<CategoryResponseDTO> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    /**
     * GET /api/categories/priorities : Récupère la liste des priorités de tâches (Haute, Moyenne, Basse).
     */
    @GetMapping("/api/categories/priorities")
    public ResponseEntity<List<PriorityResponseDTO>> getPriorities() {
        return ResponseEntity.ok(categoryService.getAllPriorities());
    }

    // --- Endpoints d'Administration (ROLE_ADMIN) ---

    /**
     * POST /api/admin/categories : Créer une nouvelle catégorie.
     */
    @PostMapping("/api/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponseDTO> createCategory(
            @Valid @RequestBody CategoryRequest request,
            Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : "ADMIN";
        CategoryResponseDTO created = categoryService.createCategory(request, adminEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/admin/categories/{id} : Modifier une catégorie existante.
     */
    @PutMapping("/api/admin/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponseDTO> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request,
            Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : "ADMIN";
        CategoryResponseDTO updated = categoryService.updateCategory(id, request, adminEmail);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/admin/categories/{id} : Supprimer une catégorie.
     */
    @DeleteMapping("/api/admin/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteCategory(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : "ADMIN";
        categoryService.deleteCategory(id, adminEmail);
        return ResponseEntity.ok(Map.of("message", "Catégorie ID " + id + " supprimée avec succès."));
    }
}

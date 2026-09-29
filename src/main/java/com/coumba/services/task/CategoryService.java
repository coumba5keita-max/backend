package com.coumba.services.task;

import com.coumba.dto.task.CategoryRequest;
import com.coumba.dto.task.CategoryResponseDTO;
import com.coumba.dto.task.PriorityResponseDTO;
import com.coumba.entities.enums.TaskPriority;
import com.coumba.entities.task.Category;
import com.coumba.repositories.task.CategoryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Initialisation automatique des catégories par défaut si la base est vide.
     * (Bug, Feature, Documentation, Amélioration)
     */
    @PostConstruct
    public void initDefaultCategories() {
        try {
            List<CategoryRequest> defaults = List.of(
                    CategoryRequest.builder().name("Bug").description("Anomalie ou dysfonctionnement à corriger").color("#E53E3E").build(),
                    CategoryRequest.builder().name("Feature").description("Nouvelle fonctionnalité logicielle").color("#3182CE").build(),
                    CategoryRequest.builder().name("Documentation").description("Rédaction et mise à jour de la documentation").color("#38A169").build(),
                    CategoryRequest.builder().name("Amélioration").description("Optimisation technique ou amélioration UX").color("#805AD5").build()
            );

            for (CategoryRequest req : defaults) {
                if (!categoryRepository.existsByNameIgnoreCase(req.getName())) {
                    categoryRepository.save(Category.builder()
                            .name(req.getName())
                            .description(req.getDescription())
                            .color(req.getColor())
                            .build());
                    log.info("Catégorie par défaut initialisée : [{}]", req.getName());
                }
            }
        } catch (Exception e) {
            log.warn("Impossible d'initialiser les catégories par défaut : {}", e.getMessage());
        }
    }

    /**
     * Crée une nouvelle catégorie de tâche.
     */
    @Transactional
    public CategoryResponseDTO createCategory(CategoryRequest request, String adminEmail) {
        log.info("AUDIT : L'administrateur [{}] crée la catégorie [{}]", adminEmail, request.getName());

        String name = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Une catégorie portant le nom \"" + name + "\" existe déjà.");
        }

        Category category = Category.builder()
                .name(name)
                .description(request.getDescription())
                .color(request.getColor() != null ? request.getColor() : "#4A5568")
                .build();

        Category saved = categoryRepository.save(category);
        log.info("AUDIT SUCCÈS : Catégorie ID [{}] créée par [{}]", saved.getId(), adminEmail);
        return CategoryResponseDTO.fromEntity(saved);
    }

    /**
     * Modifie une catégorie existante.
     */
    @Transactional
    public CategoryResponseDTO updateCategory(Long id, CategoryRequest request, String adminEmail) {
        log.info("AUDIT : L'administrateur [{}] modifie la catégorie ID [{}]", adminEmail, id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new com.coumba.exceptions.task.CategoryNotFoundException(id));

        String newName = request.getName().trim();
        if (!category.getName().equalsIgnoreCase(newName) && categoryRepository.existsByNameIgnoreCase(newName)) {
            throw new IllegalArgumentException("Une autre catégorie porte déjà le nom \"" + newName + "\".");
        }

        category.setName(newName);
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getColor() != null) {
            category.setColor(request.getColor());
        }

        Category saved = categoryRepository.save(category);
        log.info("AUDIT SUCCÈS : Catégorie ID [{}] mise à jour par [{}]", saved.getId(), adminEmail);
        return CategoryResponseDTO.fromEntity(saved);
    }

    /**
     * Supprime une catégorie après détachement sécurisé des tâches associées.
     */
    @Transactional
    public void deleteCategory(Long id, String adminEmail) {
        log.info("AUDIT : L'administrateur [{}] supprime la catégorie ID [{}]", adminEmail, id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new com.coumba.exceptions.task.CategoryNotFoundException(id));

        // 1. Dissocier de façon atomique la catégorie de toutes les tâches
        categoryRepository.detachCategoryFromTasks(id);

        // 2. Supprimer la catégorie
        categoryRepository.delete(category);
        log.info("AUDIT SUCCÈS : Catégorie ID [{}] supprimée par [{}]", id, adminEmail);
    }

    /**
     * Récupère la liste de toutes les catégories.
     */
    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Récupère une catégorie par son ID.
     */
    @Transactional(readOnly = true)
    public CategoryResponseDTO getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .map(CategoryResponseDTO::fromEntity)
                .orElseThrow(() -> new com.coumba.exceptions.task.CategoryNotFoundException(id));
    }

    /**
     * Récupère la liste des priorités de tâches configurées (Haute, Moyenne, Basse).
     */
    public List<PriorityResponseDTO> getAllPriorities() {
        return Arrays.stream(TaskPriority.values())
                .map(PriorityResponseDTO::fromEnum)
                .collect(Collectors.toList());
    }
}

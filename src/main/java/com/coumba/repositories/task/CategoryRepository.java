package com.coumba.repositories.task;

import com.coumba.entities.task.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
    Optional<Category> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Task t SET t.category = null WHERE t.category.id = :categoryId")
    void detachCategoryFromTasks(@org.springframework.data.repository.query.Param("categoryId") Long categoryId);
}

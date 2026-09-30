package com.coumba.repositories.project;

import com.coumba.entities.project.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByIsArchived(Boolean isArchived);
    boolean existsByName(String name);
    boolean existsByNameIgnoreCase(String name);
    Optional<Project> findByName(String name);
    Optional<Project> findByNameIgnoreCase(String name);

    @Query("SELECT p FROM Project p JOIN p.users u WHERE u.id = :userId")
    List<Project> findByUserId(@Param("userId") Long userId);
}

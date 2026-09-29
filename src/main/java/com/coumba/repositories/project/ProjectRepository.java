package com.coumba.repositories.project;

import com.coumba.entities.project.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByIsArchived(Boolean isArchived);
    boolean existsByName(String name);
    Optional<Project> findByName(String name);
}

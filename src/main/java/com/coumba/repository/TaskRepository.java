package com.coumba.repository;

import com.coumba.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    // Méthode personnalisée pour récupérer toutes les tâches d'un employé spécifique
    List<Task> findByEmployeeId(Long employeeId);
}
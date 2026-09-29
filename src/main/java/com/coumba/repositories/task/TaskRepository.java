package com.coumba.repositories.task;

import com.coumba.entities.task.Task;
import com.coumba.entities.enums.TaskPriority;
import com.coumba.entities.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProjectId(Long projectId);
    List<Task> findByCreatorId(Long creatorId);
    List<Task> findByStatus(TaskStatus status);
    List<Task> findByPriority(TaskPriority priority);

    long countByStatus(TaskStatus status);
    long countByPriority(TaskPriority priority);

    @Query("SELECT t FROM Task t JOIN t.assignees a WHERE a.id = :userId")
    List<Task> findByAssigneeId(@Param("userId") Long userId);

    @Query("SELECT t FROM Task t WHERE t.dueDate IS NOT NULL AND t.dueDate < :currentDate AND t.status NOT IN (com.coumba.entities.enums.TaskStatus.TERMINE, com.coumba.entities.enums.TaskStatus.CLOTURE)")
    List<Task> findOverdueTasks(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.dueDate IS NOT NULL AND t.dueDate < :currentDate AND t.status NOT IN (com.coumba.entities.enums.TaskStatus.TERMINE, com.coumba.entities.enums.TaskStatus.CLOTURE)")
    long countOverdueTasks(@Param("currentDate") LocalDate currentDate);
}

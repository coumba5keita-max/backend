package com.coumba.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String status; // Ex: "TODO", "IN_PROGRESS", "DONE"

    // Relation ManyToOne : Plusieurs tâches peuvent être assignées à un seul employé
    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;

    public Task() {
    }

    public Task(String title, String description, String status, Employee employee) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.employee = employee;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }
}
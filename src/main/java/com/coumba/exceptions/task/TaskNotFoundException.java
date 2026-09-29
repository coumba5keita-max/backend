package com.coumba.exceptions.task;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(String message) {
        super(message);
    }

    public TaskNotFoundException(Long taskId) {
        super("Tâche introuvable avec l'identifiant : " + taskId);
    }
}

package com.coumba.exceptions.project;

public class ProjectNotFoundException extends RuntimeException {
    public ProjectNotFoundException(String message) {
        super(message);
    }

    public ProjectNotFoundException(Long projectId) {
        super("Projet introuvable avec l'identifiant : " + projectId);
    }
}

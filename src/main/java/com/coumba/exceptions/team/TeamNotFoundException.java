package com.coumba.exceptions.team;

public class TeamNotFoundException extends RuntimeException {
    public TeamNotFoundException(String message) {
        super(message);
    }

    public TeamNotFoundException(Long teamId) {
        super("Équipe introuvable avec l'identifiant : " + teamId);
    }
}

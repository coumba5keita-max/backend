package com.coumba.exceptions.auth;

public class AccountDisabledException extends RuntimeException {
    public AccountDisabledException(String message) {
        super(message);
    }

    public AccountDisabledException() {
        super("Le compte utilisateur est désactivé. Veuillez contacter l'administrateur.");
    }
}

package com.coumba.exceptions.task;

public class CategoryNotFoundException extends RuntimeException {
    public CategoryNotFoundException(String message) {
        super(message);
    }

    public CategoryNotFoundException(Long categoryId) {
        super("Catégorie introuvable avec l'identifiant : " + categoryId);
    }
}

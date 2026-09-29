package com.coumba.security;

import java.security.SecureRandom;

public final class SecurityUtils {

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()-_=+";
    private static final String ALL = UPPER + LOWER + DIGITS + SPECIAL;

    private static final SecureRandom RANDOM = new SecureRandom();

    private SecurityUtils() {
    }

    /**
     * Génère un mot de passe temporaire fort et aléatoire (14 caractères)
     * respectant les contraintes de complexité (majuscule, minuscule, chiffre, symbole).
     */
    public static String generateSecureTemporaryPassword() {
        StringBuilder password = new StringBuilder(14);

        // Garantir au moins un caractère de chaque type
        password.append(UPPER.charAt(RANDOM.nextInt(UPPER.length())));
        password.append(LOWER.charAt(RANDOM.nextInt(LOWER.length())));
        password.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        password.append(SPECIAL.charAt(RANDOM.nextInt(SPECIAL.length())));

        // Compléter jusqu'à 14 caractères avec des caractères aléatoires
        for (int i = 4; i < 14; i++) {
            password.append(ALL.charAt(RANDOM.nextInt(ALL.length())));
        }

        // Mélanger les caractères pour éviter un format prédictible
        char[] array = password.toString().toCharArray();
        for (int i = array.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }

        return new String(array);
    }
}

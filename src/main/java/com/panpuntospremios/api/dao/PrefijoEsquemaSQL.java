package com.panpuntospremios.api.dao;

/** Construye prefijos SQL seguros para el propietario de las tablas de la API. */
final class PrefijoEsquemaSQL {

    private PrefijoEsquemaSQL() {
    }

    static String desde(String esquema) {
        if (esquema == null || esquema.isBlank()) {
            return "";
        }
        String normalizado = esquema.trim().toUpperCase(java.util.Locale.ROOT);
        if (!normalizado.matches("[A-Z][A-Z0-9_$#]*")) {
            throw new IllegalArgumentException("DB_SCHEMA contiene un nombre de esquema Oracle inválido");
        }
        return normalizado + ".";
    }
}

package com.panpuntospremios.util;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionOracle {

    private static final Dotenv DOTENV = Dotenv.configure().ignoreIfMissing().load();

    // Swing y la API comparten el archivo .env local, pero deben poder usar
    // cuentas diferentes. Las variables SWING_* tienen prioridad y DB_* queda
    // como fallback para no romper configuraciones antiguas del escritorio.
    private static final String URL = primera("SWING_DB_URL", "DB_URL");
    private static final String USUARIO_SWING = valor("SWING_DB_USER");
    private static final String PASSWORD_SWING = valor("SWING_DB_PASSWORD");
    private static final String USUARIO_COMPARTIDO = valor("DB_USER");
    private static final boolean DB_COMPARTIDA_ES_API = "APP_PUNTOS_API".equalsIgnoreCase(
        USUARIO_COMPARTIDO == null ? "" : USUARIO_COMPARTIDO.trim()
    );
    private static final String USUARIO = tieneTexto(USUARIO_SWING)
        ? USUARIO_SWING : DB_COMPARTIDA_ES_API ? null : USUARIO_COMPARTIDO;
    private static final String PASSWORD = tieneTexto(USUARIO_SWING)
        ? PASSWORD_SWING : DB_COMPARTIDA_ES_API ? null : valor("DB_PASSWORD");

    public static Connection obtener() throws SQLException {
        if (URL == null || URL.isBlank()) {
            throw new SQLException("Falta configurar SWING_DB_URL o DB_URL en el entorno local.");
        }
        if (USUARIO == null || USUARIO.isBlank()) {
            throw new SQLException("Configura SWING_DB_USER y SWING_DB_PASSWORD para Swing. DB_USER=APP_PUNTOS_API es exclusivo de Spring Boot.");
        }
        if (PASSWORD == null || PASSWORD.isBlank()) {
            throw new SQLException("Falta configurar SWING_DB_PASSWORD para el usuario de Swing.");
        }
        return DriverManager.getConnection(URL, USUARIO, PASSWORD);
    }

    private static String primera(String preferida, String alternativa) {
        String valor = valor(preferida);
        return tieneTexto(valor) ? valor : valor(alternativa);
    }

    private static String valor(String nombre) {
        String valor = System.getenv(nombre);
        if (!tieneTexto(valor)) valor = DOTENV.get(nombre);
        return valor;
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}

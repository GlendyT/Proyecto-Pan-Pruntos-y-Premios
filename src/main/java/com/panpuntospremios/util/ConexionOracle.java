package com.panpuntospremios.util;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionOracle {

    private static final Dotenv DOTENV = Dotenv.configure().ignoreIfMissing().load();

    private static final String URL = DOTENV.get("DB_URL");
    private static final String USUARIO = DOTENV.get("DB_USER");
    private static final String PASSWORD = DOTENV.get("DB_PASSWORD");

    public static Connection obtener() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, PASSWORD);
    }
}

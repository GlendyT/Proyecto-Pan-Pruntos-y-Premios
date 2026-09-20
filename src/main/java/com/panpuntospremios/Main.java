package com.panpuntospremios;

import com.panpuntospremios.util.ConexionOracle;

import java.sql.Connection;

public class Main {

    public static void main(String[] args) {
        try (Connection conexion = ConexionOracle.obtener()) {
            System.out.println("Conexion exitosa a Oracle Cloud: " + conexion.getMetaData().getURL());
        } catch (Exception e) {
            System.out.println("No se pudo conectar a la base de datos.");
            e.printStackTrace();
        }
    }
}

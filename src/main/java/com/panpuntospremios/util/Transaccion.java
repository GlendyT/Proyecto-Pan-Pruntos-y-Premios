package com.panpuntospremios.util;

import java.sql.Connection;
import java.sql.SQLException;

public final class Transaccion {

    private Transaccion() {}

    @FunctionalInterface
    public interface Operacion<T> {
        T ejecutar() throws SQLException;
    }

    public static <T> T ejecutar(Connection conexion, Operacion<T> operacion) throws SQLException {
        boolean autoCommitOriginal = conexion.getAutoCommit();
        conexion.setAutoCommit(false);
        try {
            T resultado = operacion.ejecutar();
            conexion.commit();
            return resultado;
        } catch (SQLException | RuntimeException e) {
            try {
                conexion.rollback();
            } catch (SQLException errorRollback) {
                e.addSuppressed(errorRollback);
            }
            throw e;
        } finally {
            conexion.setAutoCommit(autoCommitOriginal);
        }
    }
}

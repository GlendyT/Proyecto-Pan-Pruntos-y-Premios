package com.panpuntospremios.dao;

import com.panpuntospremios.modelo.Cliente;
import com.panpuntospremios.util.ConexionOracle;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ClienteDAOOraclePersistenciaIT {

    @Test
    void conservaLosCambiosAlCerrarYReabrirOracle() throws Exception {
        assumeTrue(
            Boolean.getBoolean("oracle.it"),
            "Se ejecuta explícitamente con -Doracle.it=true y nunca como parte de la CI."
        );

        String identificador = "IT" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
        try {
            registrarClienteTemporal(identificador);

            Cliente guardado;
            try (Connection conexionReabierta = ConexionOracle.obtener()) {
                ClienteDAO dao = new ClienteDAOImpl(conexionReabierta);
                guardado = dao.buscarPorIdentificador(identificador);
                assertNotNull(guardado);

                Cliente editado = new Cliente(
                    guardado.getIdCliente(),
                    guardado.getIdentificador(),
                    "Cliente temporal editado",
                    "55550000",
                    "temporal@prueba.local",
                    guardado.getSaldoPuntos(),
                    true,
                    guardado.getFechaRegistro()
                );
                dao.actualizar(editado);
            }

            try (Connection segundaReapertura = ConexionOracle.obtener()) {
                Cliente persistido = new ClienteDAOImpl(segundaReapertura)
                    .buscarPorIdentificador(identificador);

                assertNotNull(persistido);
                assertEquals("Cliente temporal editado", persistido.getNombre());
                assertEquals("temporal@prueba.local", persistido.getEmail());
            }
        } finally {
            eliminarClienteTemporal(identificador);
        }
    }

    private void registrarClienteTemporal(String identificador) throws Exception {
        Cliente cliente = new Cliente(
            0,
            identificador,
            "Cliente temporal",
            "55550000",
            "temporal@prueba.local",
            0,
            true,
            null
        );
        try (Connection conexion = ConexionOracle.obtener()) {
            new ClienteDAOImpl(conexion).registrar(cliente);
        }
    }

    private void eliminarClienteTemporal(String identificador) throws Exception {
        try (Connection conexion = ConexionOracle.obtener();
             PreparedStatement sentencia = conexion.prepareStatement(
                 "DELETE FROM cliente WHERE identificador = ?"
             )) {
            sentencia.setString(1, identificador);
            sentencia.executeUpdate();
        }
    }
}

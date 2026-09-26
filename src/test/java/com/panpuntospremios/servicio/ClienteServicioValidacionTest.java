package com.panpuntospremios.servicio;

import com.panpuntospremios.dao.ClienteDAO;
import com.panpuntospremios.excepcion.ValidacionException;
import com.panpuntospremios.modelo.Cliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClienteServicioValidacionTest {

    private ClienteDAOFalso dao;
    private ClienteServicio servicio;

    @BeforeEach
    void preparar() {
        dao = new ClienteDAOFalso();
        servicio = new ClienteServicio(dao);
    }

    @Test
    void rechazaIdentificadorNegativo() {
        Cliente cliente = cliente(0, "-123", "María López", "55551234", "maria@correo.com");

        assertThrows(ValidacionException.class, () -> servicio.registrar(cliente));
        assertEquals(0, dao.registros.size());
    }

    @Test
    void rechazaNombreConNumeros() {
        Cliente cliente = cliente(0, "CLI003", "Cliente 123", "55551234", "cliente@correo.com");

        assertThrows(ValidacionException.class, () -> servicio.registrar(cliente));
    }

    @Test
    void rechazaTelefonoConLetras() {
        Cliente cliente = cliente(0, "CLI003", "Cliente Válido", "5555ABCD", "cliente@correo.com");

        assertThrows(ValidacionException.class, () -> servicio.registrar(cliente));
    }

    @Test
    void rechazaTelefonoDemasiadoCorto() {
        Cliente cliente = cliente(0, "CLI003", "Cliente Válido", "123", "cliente@correo.com");

        assertThrows(ValidacionException.class, () -> servicio.registrar(cliente));
    }

    @Test
    void rechazaEmailSinArrobaODominio() {
        Cliente sinArroba = cliente(0, "CLI003", "Cliente Válido", "55551234", "correoinvalido.com");
        Cliente sinDominio = cliente(0, "CLI004", "Cliente Válido", "55551234", "correo@");

        assertThrows(ValidacionException.class, () -> servicio.registrar(sinArroba));
        assertThrows(ValidacionException.class, () -> servicio.registrar(sinDominio));
    }

    @Test
    void aceptaDatosValidosConAcentosYTelefonoFormateado() {
        Cliente cliente = cliente(
            0,
            "CLI-ÁRBOL_01",
            "María-José O'Connor",
            "+52 (555) 123-4567",
            "maria.jose+cliente@correo.com"
        );

        assertDoesNotThrow(() -> servicio.registrar(cliente));
        assertEquals(1, dao.registros.size());
    }

    @Test
    void permiteTelefonoYEmailVaciosPorqueSonOpcionalesEnElEsquema() {
        Cliente cliente = cliente(0, "CLI005", "Cliente Válido", "", "");

        assertDoesNotThrow(() -> servicio.registrar(cliente));
    }

    @Test
    void aplicaLasValidacionesTambienAlEditar() {
        Cliente nombreInvalido = cliente(1, "CLI006", "12345", "55551234", "cliente@correo.com");
        Cliente telefonoInvalido = cliente(1, "CLI006", "Cliente Válido", "TELÉFONO", "cliente@correo.com");
        Cliente emailInvalido = cliente(1, "CLI006", "Cliente Válido", "55551234", "sin-arroba");

        assertThrows(ValidacionException.class, () -> servicio.actualizar(nombreInvalido));
        assertThrows(ValidacionException.class, () -> servicio.actualizar(telefonoInvalido));
        assertThrows(ValidacionException.class, () -> servicio.actualizar(emailInvalido));
    }

    @Test
    void rechazaIdentificadorDuplicado() throws Exception {
        Cliente existente = cliente(1, "CLI007", "Cliente Existente", "55551234", "existente@correo.com");
        dao.registrar(existente);

        Cliente duplicado = cliente(0, "CLI007", "Otro Cliente", "55556789", "otro@correo.com");

        assertThrows(ValidacionException.class, () -> servicio.registrar(duplicado));
    }

    private Cliente cliente(int id, String identificador, String nombre, String telefono, String email) {
        return new Cliente(id, identificador, nombre, telefono, email, 0, true, null);
    }

    private static class ClienteDAOFalso implements ClienteDAO {
        private final List<Cliente> registros = new ArrayList<>();

        @Override
        public void registrar(Cliente cliente) {
            registros.add(cliente);
        }

        @Override
        public Cliente buscarPorIdentificador(String identificador) {
            return registros.stream()
                .filter(cliente -> identificador.equals(cliente.getIdentificador()))
                .findFirst()
                .orElse(null);
        }

        @Override
        public List<Cliente> listarActivos() {
            return registros.stream().filter(Cliente::isActivo).toList();
        }

        @Override
        public void actualizar(Cliente cliente) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void desactivar(int idCliente) throws SQLException {
            throw new UnsupportedOperationException();
        }
    }
}

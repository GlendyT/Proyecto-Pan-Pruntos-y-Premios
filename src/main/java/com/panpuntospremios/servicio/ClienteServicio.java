package com.panpuntospremios.servicio;

import com.panpuntospremios.dao.ClienteDAO;
import com.panpuntospremios.excepcion.ValidacionException;
import com.panpuntospremios.modelo.Cliente;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

public class ClienteServicio {
    private static final Pattern PATRON_IDENTIFICADOR = Pattern.compile(
        "^[\\p{L}\\p{N}][\\p{L}\\p{N}_-]{0,19}$",
        Pattern.UNICODE_CHARACTER_CLASS
    );
    private static final Pattern PATRON_NOMBRE = Pattern.compile(
        "^[\\p{L}]+(?:[ '\\-][\\p{L}]+)*$",
        Pattern.UNICODE_CHARACTER_CLASS
    );
    private static final Pattern PATRON_TELEFONO = Pattern.compile("^\\+?[0-9][0-9() .-]*$");
    private static final Pattern PATRON_EMAIL = Pattern.compile(
        "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,63}$",
        Pattern.CASE_INSENSITIVE
    );

    private final ClienteDAO clienteDAO;

    public ClienteServicio(ClienteDAO clienteDAO) {
        this.clienteDAO = clienteDAO;
    }

    public void registrar(Cliente cliente) throws SQLException {
        validarCliente(cliente, true);
        if (clienteDAO.buscarPorIdentificador(cliente.getIdentificador()) != null) {
            throw new ValidacionException("Ya existe un cliente con ese identificador.");
        }
        clienteDAO.registrar(cliente);
    }

    public Cliente buscarPorIdentificador(String identificador) throws SQLException {
        validarIdentificador(identificador);
        return clienteDAO.buscarPorIdentificador(identificador);
    }

    public List<Cliente> listarActivos() throws SQLException {
        return clienteDAO.listarActivos();
    }

    public void actualizar(Cliente cliente) throws SQLException {
        validarCliente(cliente, false);
        if (cliente.getIdCliente() <= 0) {
            throw new ValidacionException("El ID del cliente debe ser mayor que cero.");
        }
        clienteDAO.actualizar(cliente);
    }

    public void desactivar(int idCliente) throws SQLException {
        if (idCliente <= 0) {
            throw new ValidacionException("El ID del cliente debe ser mayor que cero.");
        }
        clienteDAO.desactivar(idCliente);
    }

    private void validarCliente(Cliente cliente, boolean validarIdentificador) {
        if (cliente == null) {
            throw new ValidacionException("Los datos del cliente son obligatorios.");
        }
        if (validarIdentificador) {
            validarIdentificador(cliente.getIdentificador());
        }
        validarNombre(cliente.getNombre());
        validarTelefono(cliente.getTelefono());
        validarEmail(cliente.getEmail());
    }

    private void validarIdentificador(String identificador) {
        if (identificador == null || identificador.isBlank()) {
            throw new ValidacionException("El identificador es obligatorio.");
        }
        if (!PATRON_IDENTIFICADOR.matcher(identificador).matches()) {
            throw new ValidacionException(
                "El identificador debe tener de 1 a 20 caracteres, comenzar con una letra o número "
                    + "y contener únicamente letras, números, guiones o guiones bajos."
            );
        }
    }

    private void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ValidacionException("El nombre es obligatorio.");
        }
        if (nombre.length() > 100) {
            throw new ValidacionException("El nombre no puede superar los 100 caracteres.");
        }
        if (!PATRON_NOMBRE.matcher(nombre).matches()) {
            throw new ValidacionException(
                "El nombre solo puede contener letras, espacios, apóstrofes y guiones."
            );
        }
    }

    private void validarTelefono(String telefono) {
        if (telefono == null || telefono.isBlank()) {
            return;
        }
        if (telefono.length() > 20 || !PATRON_TELEFONO.matcher(telefono).matches()) {
            throw new ValidacionException(
                "El teléfono solo puede contener números, espacios, paréntesis, punto, guion o un + inicial."
            );
        }
        long cantidadDigitos = telefono.chars().filter(Character::isDigit).count();
        if (cantidadDigitos < 7 || cantidadDigitos > 15) {
            throw new ValidacionException("El teléfono debe contener entre 7 y 15 dígitos.");
        }
    }

    private void validarEmail(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        if (email.length() > 100 || !PATRON_EMAIL.matcher(email).matches()) {
            throw new ValidacionException("El correo electrónico no tiene un formato válido.");
        }
    }
}

package com.panpuntospremios.servicio;

import com.panpuntospremios.dao.ClienteDAO;
import com.panpuntospremios.modelo.Cliente;
import java.sql.SQLException;
import java.util.List;

public class ClienteServicio {
    private final ClienteDAO clienteDAO;

    public ClienteServicio(ClienteDAO clienteDAO) {
        this.clienteDAO = clienteDAO;
    }

    public void registrar(Cliente cliente) throws SQLException {
        if (cliente.getIdentificador() == null || cliente.getIdentificador().isBlank())
            throw new IllegalArgumentException("El identificador es obligatorio.");
        if (cliente.getNombre() == null || cliente.getNombre().isBlank())
            throw new IllegalArgumentException("El nombre es obligatorio.");
        if (clienteDAO.buscarPorIdentificador(cliente.getIdentificador()) != null)
            throw new IllegalArgumentException("Ya existe un cliente con ese identificador.");
        clienteDAO.registrar(cliente);
    }

    public Cliente buscarPorIdentificador(String identificador) throws SQLException {
        return clienteDAO.buscarPorIdentificador(identificador);
    }

    public List<Cliente> listarActivos() throws SQLException {
        return clienteDAO.listarActivos();
    }

    public void actualizar(Cliente cliente) throws SQLException {
        if (cliente.getNombre() == null || cliente.getNombre().isBlank())
            throw new IllegalArgumentException("El nombre es obligatorio.");
        clienteDAO.actualizar(cliente);
    }

    public void desactivar(int idCliente) throws SQLException {
        clienteDAO.desactivar(idCliente);
    }
}

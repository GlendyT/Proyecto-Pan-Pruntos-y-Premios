package com.panpuntospremios.dao;

import com.panpuntospremios.modelo.Cliente;
import java.sql.SQLException;
import java.util.List;

public interface ClienteDAO {
    void registrar(Cliente cliente) throws SQLException;
    Cliente buscarPorIdentificador(String identificador) throws SQLException;
    List<Cliente> listarActivos() throws SQLException;
    void actualizar(Cliente cliente) throws SQLException;
    void desactivar(int idCliente) throws SQLException;
}

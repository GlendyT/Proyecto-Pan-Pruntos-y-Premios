package com.panpuntospremios.dao;

import com.panpuntospremios.modelo.Canje;
import java.sql.SQLException;
import java.util.List;

public interface CanjeDAO {
    void registrar(int idCliente, int idRecompensa, int puntosUtilizados) throws SQLException;
    List<Canje> listarPorCliente(int idCliente) throws SQLException;
}

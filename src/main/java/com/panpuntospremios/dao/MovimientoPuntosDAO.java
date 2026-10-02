package com.panpuntospremios.dao;

import com.panpuntospremios.modelo.MovimientoPuntos;
import java.sql.SQLException;
import java.util.List;

public interface MovimientoPuntosDAO {
    List<MovimientoPuntos> listarPorCliente(int idCliente) throws SQLException;
}

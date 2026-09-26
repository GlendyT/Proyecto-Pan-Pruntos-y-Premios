package com.panpuntospremios.dao;

import com.panpuntospremios.modelo.MovimientoInventario;

import java.sql.SQLException;
import java.util.List;

public interface MovimientoInventarioDAO {
    void registrar(MovimientoInventario movimiento) throws SQLException;
    List<MovimientoInventario> listarRecientes() throws SQLException;
}

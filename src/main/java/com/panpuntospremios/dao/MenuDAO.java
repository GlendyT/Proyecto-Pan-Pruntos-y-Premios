package com.panpuntospremios.dao;

import com.panpuntospremios.modelo.Menu;

import java.sql.SQLException;
import java.util.List;

public interface MenuDAO {
    void registrar(Menu menu) throws SQLException;
    Menu buscarPorCodigo(String codigo) throws SQLException;
    List<Menu> listarActivos() throws SQLException;
    void actualizar(Menu menu) throws SQLException;
    void desactivar(int idMenu) throws SQLException;
}

package com.panpuntospremios.dao;

import com.panpuntospremios.modelo.Producto;

import java.sql.SQLException;
import java.util.List;

public interface ProductoDAO {
    void registrar(Producto producto) throws SQLException;
    Producto buscarPorId(int idProducto) throws SQLException;
    Producto buscarPorCodigo(String codigo) throws SQLException;
    List<Producto> listarActivos() throws SQLException;
    void actualizar(Producto producto) throws SQLException;
    void desactivar(int idProducto) throws SQLException;
    Producto bloquearPorId(int idProducto) throws SQLException;
    void actualizarExistencia(int idProducto, int nuevaExistencia) throws SQLException;
}

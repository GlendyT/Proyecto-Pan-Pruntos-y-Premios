package com.panpuntospremios.dao;

import com.panpuntospremios.modelo.Producto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAOImpl implements ProductoDAO {
    private static final String COLUMNAS =
        "id_producto, codigo, categoria, nombre, precio, existencia, activo";

    private final Connection conexion;

    public ProductoDAOImpl(Connection conexion) {
        this.conexion = conexion;
    }

    @Override
    public void registrar(Producto producto) throws SQLException {
        String sql = "INSERT INTO producto (codigo, categoria, nombre, precio, existencia) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, producto.getCodigo());
            ps.setString(2, producto.getCategoria());
            ps.setString(3, producto.getNombre());
            ps.setBigDecimal(4, producto.getPrecio());
            ps.setInt(5, producto.getExistencia());
            ps.executeUpdate();
        }
    }

    @Override
    public Producto buscarPorId(int idProducto) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM producto WHERE id_producto = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            return obtenerUno(ps);
        }
    }

    @Override
    public Producto buscarPorCodigo(String codigo) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM producto WHERE codigo = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, codigo);
            return obtenerUno(ps);
        }
    }

    @Override
    public List<Producto> listarActivos() throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM producto WHERE activo = 'S' ORDER BY categoria, nombre";
        List<Producto> productos = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                productos.add(mapear(rs));
            }
        }
        return productos;
    }

    @Override
    public void actualizar(Producto producto) throws SQLException {
        String sql = "UPDATE producto SET nombre = ?, precio = ? WHERE id_producto = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, producto.getNombre());
            ps.setBigDecimal(2, producto.getPrecio());
            ps.setInt(3, producto.getIdProducto());
            ps.executeUpdate();
        }
    }

    @Override
    public void desactivar(int idProducto) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(
            "UPDATE producto SET activo = 'N' WHERE id_producto = ?"
        )) {
            ps.setInt(1, idProducto);
            ps.executeUpdate();
        }
    }

    @Override
    public Producto bloquearPorId(int idProducto) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM producto WHERE id_producto = ? FOR UPDATE";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idProducto);
            return obtenerUno(ps);
        }
    }

    @Override
    public void actualizarExistencia(int idProducto, int nuevaExistencia) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(
            "UPDATE producto SET existencia = ? WHERE id_producto = ?"
        )) {
            ps.setInt(1, nuevaExistencia);
            ps.setInt(2, idProducto);
            ps.executeUpdate();
        }
    }

    private Producto obtenerUno(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? mapear(rs) : null;
        }
    }

    private Producto mapear(ResultSet rs) throws SQLException {
        return new Producto(
            rs.getInt("id_producto"),
            rs.getString("codigo"),
            rs.getString("categoria"),
            rs.getString("nombre"),
            rs.getBigDecimal("precio"),
            rs.getInt("existencia"),
            "S".equals(rs.getString("activo"))
        );
    }
}

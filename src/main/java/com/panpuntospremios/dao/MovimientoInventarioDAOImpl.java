package com.panpuntospremios.dao;

import com.panpuntospremios.modelo.MovimientoInventario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MovimientoInventarioDAOImpl implements MovimientoInventarioDAO {
    private final Connection conexion;

    public MovimientoInventarioDAOImpl(Connection conexion) {
        this.conexion = conexion;
    }

    @Override
    public void registrar(MovimientoInventario movimiento) throws SQLException {
        String sql = """
            INSERT INTO movimiento_inventario
                   (id_producto, tipo, cantidad, existencia_resultante)
            VALUES (?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, movimiento.getIdProducto());
            ps.setString(2, movimiento.getTipo());
            ps.setInt(3, movimiento.getCantidad());
            ps.setInt(4, movimiento.getExistenciaResultante());
            ps.executeUpdate();
        }
    }

    @Override
    public List<MovimientoInventario> listarRecientes() throws SQLException {
        String sql = """
            SELECT id_movimiento, id_producto, fecha, tipo, cantidad, existencia_resultante
              FROM movimiento_inventario
             ORDER BY fecha DESC, id_movimiento DESC
            """;
        List<MovimientoInventario> movimientos = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                movimientos.add(new MovimientoInventario(
                    rs.getInt("id_movimiento"),
                    rs.getInt("id_producto"),
                    rs.getTimestamp("fecha"),
                    rs.getString("tipo"),
                    rs.getInt("cantidad"),
                    rs.getInt("existencia_resultante")
                ));
            }
        }
        return movimientos;
    }
}

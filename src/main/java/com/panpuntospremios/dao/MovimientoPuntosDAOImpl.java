package com.panpuntospremios.dao;

import com.panpuntospremios.modelo.MovimientoPuntos;
import java.sql.*;
import java.util.*;

public class MovimientoPuntosDAOImpl implements MovimientoPuntosDAO {
    private final Connection conexion;

    public MovimientoPuntosDAOImpl(Connection conexion) { this.conexion = conexion; }

    @Override
    public List<MovimientoPuntos> listarPorCliente(int idCliente) throws SQLException {
        List<MovimientoPuntos> lista = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(
                "SELECT id_movimiento, id_cliente, fecha, tipo, puntos, referencia" +
                " FROM movimiento_puntos WHERE id_cliente = ?" +
                " ORDER BY fecha DESC, id_movimiento DESC")) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new MovimientoPuntos(
                        rs.getInt("id_movimiento"),
                        rs.getInt("id_cliente"),
                        rs.getTimestamp("fecha"),
                        rs.getString("tipo"),
                        rs.getInt("puntos"),
                        rs.getString("referencia")
                    ));
                }
            }
        }
        return lista;
    }
}

package com.panpuntospremios.dao;

import com.panpuntospremios.modelo.Canje;
import java.sql.*;
import java.util.*;

public class CanjeDAOImpl implements CanjeDAO {
    private final Connection conexion;

    public CanjeDAOImpl(Connection conexion) { this.conexion = conexion; }

    @Override
    public void registrar(int idCliente, int idRecompensa, int puntosUtilizados) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(
                "INSERT INTO canje (id_cliente, id_recompensa, puntos_utilizados) VALUES (?, ?, ?)")) {
            ps.setInt(1, idCliente);
            ps.setInt(2, idRecompensa);
            ps.setInt(3, puntosUtilizados);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Canje> listarPorCliente(int idCliente) throws SQLException {
        List<Canje> lista = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(
                "SELECT id_canje, id_cliente, id_recompensa, fecha, puntos_utilizados" +
                " FROM canje WHERE id_cliente = ? ORDER BY fecha DESC, id_canje DESC")) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Canje(
                        rs.getInt("id_canje"),
                        rs.getInt("id_cliente"),
                        rs.getInt("id_recompensa"),
                        rs.getTimestamp("fecha"),
                        rs.getInt("puntos_utilizados")
                    ));
                }
            }
        }
        return lista;
    }
}

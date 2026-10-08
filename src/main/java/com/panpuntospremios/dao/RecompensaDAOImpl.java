package com.panpuntospremios.dao;

import com.panpuntospremios.modelo.Recompensa;
import java.sql.*;
import java.util.*;

public class RecompensaDAOImpl implements RecompensaDAO {
    private static final String COLUMNAS =
        "id_recompensa, nombre, puntos_necesarios, id_producto, id_menu, activo";
    private final Connection conexion;

    public RecompensaDAOImpl(Connection conexion) { this.conexion = conexion; }

    @Override
    public List<Recompensa> listarActivas() throws SQLException {
        List<Recompensa> lista = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(
                "SELECT " + COLUMNAS + " FROM recompensa WHERE activo = 'S' ORDER BY puntos_necesarios");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    @Override
    public Recompensa buscarPorId(int idRecompensa) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(
                "SELECT " + COLUMNAS + " FROM recompensa WHERE id_recompensa = ?")) {
            ps.setInt(1, idRecompensa);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    @Override
    public void registrar(Recompensa r) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(
                "INSERT INTO recompensa (nombre, puntos_necesarios, id_producto, id_menu) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, r.getNombre());
            ps.setInt(2, r.getPuntosNecesarios());
            if (r.getIdProducto() != null) ps.setInt(3, r.getIdProducto()); else ps.setNull(3, Types.INTEGER);
            if (r.getIdMenu() != null) ps.setInt(4, r.getIdMenu()); else ps.setNull(4, Types.INTEGER);
            ps.executeUpdate();
        }
    }

    @Override
    public void actualizar(Recompensa r) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(
                "UPDATE recompensa SET nombre = ?, puntos_necesarios = ? WHERE id_recompensa = ?")) {
            ps.setString(1, r.getNombre());
            ps.setInt(2, r.getPuntosNecesarios());
            ps.setInt(3, r.getIdRecompensa());
            ps.executeUpdate();
        }
    }

    @Override
    public void desactivar(int idRecompensa) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(
                "UPDATE recompensa SET activo = 'N' WHERE id_recompensa = ?")) {
            ps.setInt(1, idRecompensa);
            ps.executeUpdate();
        }
    }

    private Recompensa mapear(ResultSet rs) throws SQLException {
        int idRecompensa = rs.getInt("id_recompensa");
        String nombre = rs.getString("nombre");
        int puntosNecesarios = rs.getInt("puntos_necesarios");
        int rawProd = rs.getInt("id_producto");
        Integer idProducto = rs.wasNull() ? null : rawProd;
        int rawMenu = rs.getInt("id_menu");
        Integer idMenu = rs.wasNull() ? null : rawMenu;
        boolean activo = "S".equals(rs.getString("activo"));
        return new Recompensa(idRecompensa, nombre, puntosNecesarios, idProducto, idMenu, activo);
    }
}

package com.panpuntospremios.dao;

import com.panpuntospremios.modelo.Menu;
import com.panpuntospremios.modelo.Producto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MenuDAOImpl implements MenuDAO {
    private static final String SELECT_COMPLETO = """
        SELECT m.id_menu, m.codigo, m.nombre, m.precio, m.activo,
               p.id_producto, p.codigo AS codigo_producto, p.categoria,
               p.nombre AS nombre_producto, p.precio AS precio_producto,
               p.existencia, p.activo AS producto_activo
          FROM menu m
          LEFT JOIN menu_detalle md ON md.id_menu = m.id_menu
          LEFT JOIN producto p ON p.id_producto = md.id_producto
        """;

    private final Connection conexion;

    public MenuDAOImpl(Connection conexion) {
        this.conexion = conexion;
    }

    @Override
    public void registrar(Menu menu) throws SQLException {
        String sql = "INSERT INTO menu (codigo, nombre, precio) VALUES (?, ?, ?)";
        int idMenu;
        try (PreparedStatement ps = conexion.prepareStatement(sql, new String[]{"ID_MENU"})) {
            ps.setString(1, menu.getCodigo());
            ps.setString(2, menu.getNombre());
            ps.setBigDecimal(3, menu.getPrecio());
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                idMenu = claves.next() ? claves.getInt(1) : buscarIdPorCodigo(menu.getCodigo());
            }
        }
        insertarComponentes(idMenu, menu.getComponentes());
    }

    @Override
    public Menu buscarPorCodigo(String codigo) throws SQLException {
        String sql = SELECT_COMPLETO + " WHERE m.codigo = ? ORDER BY p.categoria";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, codigo);
            List<Menu> menus = consultar(ps);
            return menus.isEmpty() ? null : menus.get(0);
        }
    }

    @Override
    public List<Menu> listarActivos() throws SQLException {
        String sql = SELECT_COMPLETO + " WHERE m.activo = 'S' ORDER BY m.nombre, p.categoria";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            return consultar(ps);
        }
    }

    @Override
    public void actualizar(Menu menu) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(
            "UPDATE menu SET nombre = ?, precio = ? WHERE id_menu = ?"
        )) {
            ps.setString(1, menu.getNombre());
            ps.setBigDecimal(2, menu.getPrecio());
            ps.setInt(3, menu.getIdMenu());
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conexion.prepareStatement(
            "DELETE FROM menu_detalle WHERE id_menu = ?"
        )) {
            ps.setInt(1, menu.getIdMenu());
            ps.executeUpdate();
        }
        insertarComponentes(menu.getIdMenu(), menu.getComponentes());
    }

    @Override
    public void desactivar(int idMenu) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(
            "UPDATE menu SET activo = 'N' WHERE id_menu = ?"
        )) {
            ps.setInt(1, idMenu);
            ps.executeUpdate();
        }
    }

    private int buscarIdPorCodigo(String codigo) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(
            "SELECT id_menu FROM menu WHERE codigo = ?"
        )) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("No se pudo recuperar el ID del menú registrado.");
                }
                return rs.getInt(1);
            }
        }
    }

    private void insertarComponentes(int idMenu, List<Producto> componentes) throws SQLException {
        String sql = "INSERT INTO menu_detalle (id_menu, id_producto, rol) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            for (Producto producto : componentes) {
                ps.setInt(1, idMenu);
                ps.setInt(2, producto.getIdProducto());
                ps.setString(3, producto.getCategoria());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private List<Menu> consultar(PreparedStatement ps) throws SQLException {
        Map<Integer, Menu> menus = new LinkedHashMap<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int idMenu = rs.getInt("id_menu");
                Menu menu = menus.computeIfAbsent(idMenu, id -> {
                    try {
                        return new Menu(
                            id,
                            rs.getString("codigo"),
                            rs.getString("nombre"),
                            rs.getBigDecimal("precio"),
                            "S".equals(rs.getString("activo"))
                        );
                    } catch (SQLException e) {
                        throw new ErrorMapeo(e);
                    }
                });
                int idProducto = rs.getInt("id_producto");
                if (!rs.wasNull()) {
                    menu.getComponentes().add(new Producto(
                        idProducto,
                        rs.getString("codigo_producto"),
                        rs.getString("categoria"),
                        rs.getString("nombre_producto"),
                        rs.getBigDecimal("precio_producto"),
                        rs.getInt("existencia"),
                        "S".equals(rs.getString("producto_activo"))
                    ));
                }
            }
        } catch (ErrorMapeo e) {
            throw e.sqlException;
        }
        return new ArrayList<>(menus.values());
    }

    private static class ErrorMapeo extends RuntimeException {
        private final SQLException sqlException;

        private ErrorMapeo(SQLException sqlException) {
            this.sqlException = sqlException;
        }
    }
}

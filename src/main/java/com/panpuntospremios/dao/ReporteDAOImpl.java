package com.panpuntospremios.dao;

import com.panpuntospremios.modelo.ReporteDatos;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReporteDAOImpl implements ReporteDAO {
    private final Connection conexion;
    public ReporteDAOImpl(Connection conexion) { this.conexion = conexion; }

    @Override public ReporteDatos consultarVentas(LocalDate desde, LocalDate hasta, String estado) throws SQLException {
        StringBuilder sql = new StringBuilder("""
            SELECT p.id_pedido, p.fecha, c.identificador, c.nombre AS cliente,
                   p.estado, p.metodo_pago, p.total, p.puntos_generados
              FROM pedido p JOIN cliente c ON c.id_cliente = p.id_cliente
             WHERE p.estado IN ('PAGADO', 'ENTREGADO')
            """);
        List<Object> valores = new ArrayList<>();
        agregarFechas(sql, valores, "p.fecha", desde, hasta);
        if (!"TODOS".equals(estado)) { sql.append(" AND p.estado = ?"); valores.add(estado); }
        sql.append(" ORDER BY p.fecha DESC, p.id_pedido DESC");
        return consultar("Ventas", List.of("Pedido", "Fecha pedido", "Identificador", "Cliente", "Estado", "Método de pago", "Total", "Puntos"), sql.toString(), valores);
    }

    @Override public ReporteDatos consultarInventario(String estado) throws SQLException {
        String sql = "SELECT id_producto, codigo, categoria, nombre, precio, existencia, activo FROM producto";
        if (!"TODOS".equals(estado)) sql += " WHERE activo = ?";
        sql += " ORDER BY categoria, nombre, id_producto";
        return consultar("Inventario", List.of("ID", "Código", "Categoría", "Producto", "Precio", "Existencia", "Activo"),
            sql, "TODOS".equals(estado) ? List.of() : List.of(estado));
    }

    @Override public ReporteDatos consultarMovimientosInventario(LocalDate desde, LocalDate hasta, String tipo) throws SQLException {
        StringBuilder sql = new StringBuilder("""
            SELECT m.id_movimiento, m.fecha, m.tipo, p.codigo, p.nombre,
                   m.cantidad, m.existencia_resultante
              FROM movimiento_inventario m JOIN producto p ON p.id_producto = m.id_producto
             WHERE 1 = 1
            """);
        List<Object> valores = new ArrayList<>();
        agregarFechas(sql, valores, "m.fecha", desde, hasta);
        if (!"TODOS".equals(tipo)) { sql.append(" AND m.tipo = ?"); valores.add(tipo); }
        sql.append(" ORDER BY m.fecha DESC, m.id_movimiento DESC");
        return consultar("Movimientos de inventario", List.of("ID movimiento", "Fecha", "Tipo", "Código producto", "Producto", "Cantidad", "Existencia resultante"), sql.toString(), valores);
    }

    @Override public ReporteDatos consultarMovimientosPuntos(LocalDate desde, LocalDate hasta, String tipo) throws SQLException {
        StringBuilder sql = new StringBuilder("""
            SELECT m.id_movimiento, m.fecha, c.identificador, c.nombre,
                   m.tipo, m.puntos, m.referencia
              FROM movimiento_puntos m JOIN cliente c ON c.id_cliente = m.id_cliente
             WHERE 1 = 1
            """);
        List<Object> valores = new ArrayList<>();
        agregarFechas(sql, valores, "m.fecha", desde, hasta);
        if (!"TODOS".equals(tipo)) { sql.append(" AND m.tipo = ?"); valores.add(tipo); }
        sql.append(" ORDER BY m.fecha DESC, m.id_movimiento DESC");
        return consultar("Movimientos de puntos", List.of("ID movimiento", "Fecha", "Identificador", "Cliente", "Tipo", "Puntos", "Referencia"), sql.toString(), valores);
    }

    private static void agregarFechas(StringBuilder sql, List<Object> valores, String columna,
                                      LocalDate desde, LocalDate hasta) {
        if (desde != null) { sql.append(" AND ").append(columna).append(" >= ?"); valores.add(java.sql.Date.valueOf(desde)); }
        if (hasta != null) { sql.append(" AND ").append(columna).append(" < ?"); valores.add(java.sql.Date.valueOf(hasta.plusDays(1))); }
    }

    private ReporteDatos consultar(String titulo, List<String> columnas, String sql, List<Object> valores) throws SQLException {
        List<List<Object>> filas = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            for (int i = 0; i < valores.size(); i++) ps.setObject(i + 1, valores.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    List<Object> fila = new ArrayList<>(columnas.size());
                    for (int i = 1; i <= columnas.size(); i++) fila.add(rs.getObject(i));
                    filas.add(fila);
                }
            }
        }
        return new ReporteDatos(titulo, columnas, filas);
    }
}

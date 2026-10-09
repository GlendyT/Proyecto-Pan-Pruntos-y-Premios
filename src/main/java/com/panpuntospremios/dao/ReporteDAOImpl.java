package com.panpuntospremios.dao;

import com.panpuntospremios.modelo.ReporteDatos;
import java.math.BigDecimal;
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

    @Override
    public ReporteDatos consultarVentasPorPeriodo(LocalDate desde, LocalDate hasta, String metodoPago) throws SQLException {
        StringBuilder sql = new StringBuilder("""
            SELECT p.id_pedido, p.fecha, c.identificador, c.nombre AS cliente,
                   p.estado, p.metodo_pago, p.total, p.puntos_generados
              FROM pedido p JOIN cliente c ON c.id_cliente = p.id_cliente
             WHERE p.estado IN ('PAGADO', 'ENTREGADO')
            """);
        List<Object> valores = new ArrayList<>();
        agregarFechas(sql, valores, "p.fecha", desde, hasta);
        if (metodoPago != null && !"TODOS".equals(metodoPago)) {
            sql.append(" AND p.metodo_pago = ?");
            valores.add(metodoPago);
        }
        sql.append(" ORDER BY p.fecha DESC, p.id_pedido DESC");

        List<List<Object>> filas = new ArrayList<>();
        BigDecimal totalEfectivo = BigDecimal.ZERO;
        BigDecimal totalTarjeta = BigDecimal.ZERO;
        BigDecimal totalGeneral = BigDecimal.ZERO;

        try (PreparedStatement ps = conexion.prepareStatement(sql.toString())) {
            for (int i = 0; i < valores.size(); i++) {
                ps.setObject(i + 1, valores.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idPedido = rs.getInt("id_pedido");
                    Date fecha = rs.getDate("fecha");
                    String identificador = rs.getString("identificador");
                    String cliente = rs.getString("cliente");
                    String estado = rs.getString("estado");
                    String metodo = rs.getString("metodo_pago");
                    BigDecimal total = rs.getBigDecimal("total");
                    int puntos = rs.getInt("puntos_generados");

                    BigDecimal monto = total != null ? total : BigDecimal.ZERO;
                    totalGeneral = totalGeneral.add(monto);
                    if ("EFECTIVO".equalsIgnoreCase(metodo)) {
                        totalEfectivo = totalEfectivo.add(monto);
                    } else if ("TARJETA".equalsIgnoreCase(metodo)) {
                        totalTarjeta = totalTarjeta.add(monto);
                    }

                    filas.add(List.of(idPedido, fecha, identificador, cliente, estado, metodo, monto, puntos));
                }
            }
        }

        String titulo = String.format("Ventas por período (Total: Q%.2f | Efectivo: Q%.2f | Tarjeta: Q%.2f)",
            totalGeneral, totalEfectivo, totalTarjeta);
        List<String> columnas = List.of("Pedido", "Fecha pedido", "Identificador", "Cliente", "Estado", "Método de pago", "Total", "Puntos");
        return new ReporteDatos(titulo, columnas, filas);
    }

    @Override
    public ReporteDatos consultarProductosMasVendidos() throws SQLException {
        String sql = """
            SELECT t.codigo, t.nombre, t.categoria, t.unidades_individuales, t.unidades_en_menus,
                   (t.unidades_individuales + t.unidades_en_menus) AS total_unidades
              FROM (
                  SELECT p.codigo, p.nombre, p.categoria,
                         COALESCE((
                             SELECT SUM(pd.cantidad)
                               FROM pedido_detalle pd
                               JOIN pedido ped ON ped.id_pedido = pd.id_pedido
                              WHERE pd.id_producto = p.id_producto
                                AND pd.tipo_item = 'PRODUCTO'
                                AND ped.estado IN ('PAGADO', 'ENTREGADO')
                         ), 0) AS unidades_individuales,
                         COALESCE((
                             SELECT SUM(pd.cantidad)
                               FROM pedido_detalle pd
                               JOIN pedido ped ON ped.id_pedido = pd.id_pedido
                               JOIN menu_detalle md ON md.id_menu = pd.id_menu
                              WHERE md.id_producto = p.id_producto
                                AND pd.tipo_item = 'MENU'
                                AND ped.estado IN ('PAGADO', 'ENTREGADO')
                         ), 0) AS unidades_en_menus
                    FROM producto p
              ) t
             ORDER BY total_unidades DESC, t.nombre ASC
            """;
        List<List<Object>> filas = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String codigo = rs.getString("codigo");
                String nombre = rs.getString("nombre");
                String categoria = rs.getString("categoria");
                int individuales = rs.getInt("unidades_individuales");
                int enMenus = rs.getInt("unidades_en_menus");
                int total = rs.getInt("total_unidades");
                filas.add(List.of(codigo, nombre, categoria, individuales, enMenus, total));
            }
        }
        return new ReporteDatos(
            "Productos más vendidos",
            List.of("Código", "Producto", "Categoría", "Uds. individuales", "Uds. en menús", "Total unidades vendidas"),
            filas
        );
    }

    @Override
    public ReporteDatos consultarBajoInventario() throws SQLException {
        String sql = """
            SELECT id_producto, codigo, categoria, nombre, precio, existencia,
                   CASE WHEN existencia = 0 THEN 'AGOTADO' ELSE 'BAJO INVENTARIO' END AS estado_stock
              FROM producto
             WHERE existencia <= 5
             ORDER BY existencia ASC, categoria ASC, nombre ASC
            """;
        return consultar(
            "Productos agotados o con bajo inventario",
            List.of("ID", "Código", "Categoría", "Producto", "Precio", "Existencia", "Estado stock"),
            sql,
            List.of()
        );
    }

    @Override
    public ReporteDatos consultarPuntosPorCliente() throws SQLException {
        String sql = """
            SELECT id_cliente, identificador, nombre, telefono, email, saldo_puntos,
                   CASE WHEN activo = 'S' THEN 'ACTIVO' ELSE 'INACTIVO' END AS estado
              FROM cliente
             ORDER BY saldo_puntos DESC, nombre ASC
            """;
        return consultar(
            "Puntos acumulados por cliente",
            List.of("ID", "Identificador", "Nombre", "Teléfono", "Email", "Saldo puntos", "Estado"),
            sql,
            List.of()
        );
    }

    @Override
    public ReporteDatos consultarHistorialCanjes() throws SQLException {
        String sql = """
            SELECT c.id_canje, c.fecha, cl.identificador, cl.nombre AS cliente,
                   r.nombre AS recompensa, c.puntos_utilizados
              FROM canje c
              JOIN cliente cl ON cl.id_cliente = c.id_cliente
              JOIN recompensa r ON r.id_recompensa = c.id_recompensa
             ORDER BY c.fecha DESC, c.id_canje DESC
            """;
        return consultar(
            "Historial de canjes",
            List.of("ID canje", "Fecha", "Identificador", "Cliente", "Recompensa", "Puntos utilizados"),
            sql,
            List.of()
        );
    }

    @Override
    public ReporteDatos consultarVentasMenus() throws SQLException {
        String sql = """
            SELECT m.id_menu, m.codigo, m.nombre AS menu, m.precio,
                   COALESCE(SUM(CASE WHEN p.estado IN ('PAGADO', 'ENTREGADO') THEN pd.cantidad ELSE 0 END), 0) AS cantidad_vendida,
                   COALESCE(SUM(CASE WHEN p.estado IN ('PAGADO', 'ENTREGADO') THEN pd.subtotal ELSE 0 END), 0) AS total_recaudado
              FROM menu m
              LEFT JOIN pedido_detalle pd ON pd.id_menu = m.id_menu AND pd.tipo_item = 'MENU'
              LEFT JOIN pedido p ON p.id_pedido = pd.id_pedido
             GROUP BY m.id_menu, m.codigo, m.nombre, m.precio
             ORDER BY cantidad_vendida DESC, total_recaudado DESC, m.nombre ASC
            """;
        return consultar(
            "Ventas de menús completos",
            List.of("ID menú", "Código", "Menú", "Precio actual", "Cantidad vendida", "Importe generado"),
            sql,
            List.of()
        );
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

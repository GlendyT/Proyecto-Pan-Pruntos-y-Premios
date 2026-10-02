package com.panpuntospremios.dao;

import com.panpuntospremios.modelo.Pedido;
import com.panpuntospremios.modelo.PedidoDetalle;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PedidoDAOImpl implements PedidoDAO {
    private final Connection conexion;

    public PedidoDAOImpl(Connection conexion) {
        this.conexion = conexion;
    }

    @Override
    public int guardar(Pedido pedido) throws SQLException {
        String sql = "INSERT INTO pedido (id_cliente, total, puntos_generados) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql, new String[]{"ID_PEDIDO"})) {
            ps.setInt(1, pedido.getIdCliente());
            ps.setBigDecimal(2, pedido.getTotal());
            ps.setInt(3, pedido.getPuntosGenerados());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("No se pudo obtener el ID del pedido generado.");
    }

    @Override
    public void guardarDetalle(PedidoDetalle detalle) throws SQLException {
        String sql = "INSERT INTO pedido_detalle (id_pedido, tipo_item, id_producto, id_menu, cantidad, precio_unitario, subtotal, puntos) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, detalle.getIdPedido());
            ps.setString(2, detalle.getTipoItem());
            if (detalle.getIdProducto() != null) ps.setInt(3, detalle.getIdProducto()); else ps.setNull(3, Types.INTEGER);
            if (detalle.getIdMenu() != null) ps.setInt(4, detalle.getIdMenu()); else ps.setNull(4, Types.INTEGER);
            ps.setInt(5, detalle.getCantidad());
            ps.setBigDecimal(6, detalle.getPrecioUnitario());
            ps.setBigDecimal(7, detalle.getSubtotal());
            ps.setInt(8, detalle.getPuntos());
            ps.executeUpdate();
        }
    }

    @Override
    public Pedido buscarPorId(int idPedido) throws SQLException {
        String sql = "SELECT id_pedido, id_cliente, fecha, estado, total, puntos_generados, metodo_pago, referencia_pago FROM pedido WHERE id_pedido = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    @Override
    public List<Pedido> listarPendientesPorCliente(int idCliente) throws SQLException {
        String sql = "SELECT id_pedido, id_cliente, fecha, estado, total, puntos_generados, metodo_pago, referencia_pago FROM pedido WHERE id_cliente = ? AND estado = 'PENDIENTE' ORDER BY fecha DESC";
        List<Pedido> lista = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    @Override
    public List<Pedido> listarPorCliente(int idCliente) throws SQLException {
        String sql = "SELECT id_pedido, id_cliente, fecha, estado, total, puntos_generados, metodo_pago, referencia_pago FROM pedido WHERE id_cliente = ? ORDER BY fecha DESC, id_pedido DESC";
        List<Pedido> lista = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    @Override
    public List<PedidoDetalle> listarDetalle(int idPedido) throws SQLException {
        String sql = "SELECT id_detalle, id_pedido, tipo_item, id_producto, id_menu, cantidad, precio_unitario, subtotal, puntos FROM pedido_detalle WHERE id_pedido = ? ORDER BY id_detalle";
        List<PedidoDetalle> lista = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idPedido);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new PedidoDetalle(
                        rs.getInt("id_detalle"),
                        rs.getInt("id_pedido"),
                        rs.getString("tipo_item"),
                        rs.getObject("id_producto") != null ? rs.getInt("id_producto") : null,
                        rs.getObject("id_menu") != null ? rs.getInt("id_menu") : null,
                        rs.getInt("cantidad"),
                        rs.getBigDecimal("precio_unitario"),
                        rs.getBigDecimal("subtotal"),
                        rs.getInt("puntos")
                    ));
                }
            }
        }
        return lista;
    }

    @Override
    public void cambiarEstado(int idPedido, String estado) throws SQLException {
        String sql = "UPDATE pedido SET estado = ? WHERE id_pedido = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setInt(2, idPedido);
            ps.executeUpdate();
        }
    }

    private Pedido mapear(ResultSet rs) throws SQLException {
        return new Pedido(
            rs.getInt("id_pedido"),
            rs.getInt("id_cliente"),
            rs.getTimestamp("fecha"),
            rs.getString("estado"),
            rs.getBigDecimal("total"),
            rs.getInt("puntos_generados"),
            rs.getString("metodo_pago"),
            rs.getString("referencia_pago")
        );
    }
}

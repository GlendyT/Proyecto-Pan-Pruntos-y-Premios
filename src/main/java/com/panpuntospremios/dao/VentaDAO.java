package com.panpuntospremios.dao;

import com.panpuntospremios.modelo.*;
import com.panpuntospremios.excepcion.ValidacionException;
import java.sql.*;

/** Operaciones que comparten la conexión y los bloqueos de la transacción. */
public class VentaDAO {
    private final Connection conexion;
    public VentaDAO(Connection conexion) { this.conexion = conexion; }

    public int bloquearCliente(int id) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(
                "SELECT saldo_puntos, activo FROM cliente WHERE id_cliente = ? FOR UPDATE")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !"S".equals(rs.getString("activo")))
                    throw new ValidacionException("El cliente no existe o está inactivo.");
                return rs.getInt("saldo_puntos");
            }
        }
    }

    public Pedido bloquearPedido(int id) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(
                "SELECT id_pedido FROM pedido WHERE id_pedido = ? FOR UPDATE")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new ValidacionException("No se encontró el pedido.");
            }
        }
        return new PedidoDAOImpl(conexion).buscarPorId(id);
    }

    public Menu bloquearMenu(int id) throws SQLException {
        String codigo;
        try (PreparedStatement ps = conexion.prepareStatement(
                "SELECT codigo, activo FROM menu WHERE id_menu = ? FOR UPDATE")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !"S".equals(rs.getString("activo")))
                    throw new ValidacionException("El menú no existe o está inactivo.");
                codigo = rs.getString("codigo");
            }
        }
        Menu menu = new MenuDAOImpl(conexion).buscarPorCodigo(codigo);
        if (menu.getComponentes().size() != 3 ||
                !menu.getComponentes().stream().map(Producto::getCategoria).collect(java.util.stream.Collectors.toSet())
                    .equals(java.util.Set.of("SANDWICH", "BEBIDA", "ACOMPANAMIENTO")))
            throw new ValidacionException("El menú debe contener un producto de cada categoría.");
        return menu;
    }

    public void acreditar(int cliente, int saldo, int puntos, int pedido) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement("UPDATE cliente SET saldo_puntos = ? WHERE id_cliente = ?")) {
            ps.setInt(1, Math.addExact(saldo, puntos)); ps.setInt(2, cliente); ps.executeUpdate();
        }
        try (PreparedStatement ps = conexion.prepareStatement(
                "INSERT INTO movimiento_puntos (id_cliente, tipo, puntos, referencia) VALUES (?, 'ACUMULACION', ?, ?)")) {
            ps.setInt(1, cliente); ps.setInt(2, puntos); ps.setString(3, "PEDIDO:" + pedido); ps.executeUpdate();
        }
    }

    public void pagar(int id, String metodo, String referencia) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(
                "UPDATE pedido SET estado = 'PAGADO', metodo_pago = ?, referencia_pago = ? WHERE id_pedido = ? AND estado = 'PENDIENTE'")) {
            ps.setString(1, metodo); ps.setString(2, referencia); ps.setInt(3, id);
            if (ps.executeUpdate() != 1) throw new ValidacionException("El pedido ya no está pendiente.");
        }
    }
}

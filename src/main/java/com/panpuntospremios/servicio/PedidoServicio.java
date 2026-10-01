package com.panpuntospremios.servicio;

import com.panpuntospremios.dao.PedidoDAO;
import com.panpuntospremios.dao.ProductoDAO;
import com.panpuntospremios.excepcion.StockInsuficienteException;
import com.panpuntospremios.modelo.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PedidoServicio {
    private final PedidoDAO pedidoDAO;
    private final ProductoDAO productoDAO;
    private final Connection conexion;

    public PedidoServicio(PedidoDAO pedidoDAO, ProductoDAO productoDAO, Connection conexion) {
        this.pedidoDAO = pedidoDAO;
        this.productoDAO = productoDAO;
        this.conexion = conexion;
    }

    public int crearPedido(Pedido pedido, List<ItemVenta> items) throws SQLException, StockInsuficienteException {
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("El pedido debe tener al menos un ítem.");

        try {
            conexion.setAutoCommit(false);

            validarStock(items);

            int idPedido = pedidoDAO.guardar(pedido);
            for (ItemVenta item : items) {
                pedidoDAO.guardarDetalle(construirDetalle(idPedido, item));
            }

            conexion.commit();
            return idPedido;
        } catch (SQLException | StockInsuficienteException e) {
            conexion.rollback();
            throw e;
        } finally {
            conexion.setAutoCommit(true);
        }
    }

    private void validarStock(List<ItemVenta> items) throws SQLException, StockInsuficienteException {
        // Consolidar cantidades requeridas por producto (incluyendo componentes de menús)
        Map<Integer, Integer> requerido = new HashMap<>();
        for (ItemVenta item : items) {
            if (item instanceof ProductoIndividual pi) {
                int id = pi.getProducto().getIdProducto();
                requerido.merge(id, pi.getCantidad(), Integer::sum);
            } else if (item instanceof MenuCompleto mc) {
                for (Producto componente : mc.getMenu().getComponentes()) {
                    requerido.merge(componente.getIdProducto(), mc.getCantidad(), Integer::sum);
                }
            }
        }
        // Verificar existencias para cada producto requerido
        for (Map.Entry<Integer, Integer> entrada : requerido.entrySet()) {
            Producto producto = productoDAO.bloquearPorId(entrada.getKey());
            if (producto == null || producto.getExistencia() < entrada.getValue()) {
                String nombre = producto != null ? producto.getNombre() : "ID " + entrada.getKey();
                int disponible = producto != null ? producto.getExistencia() : 0;
                throw new StockInsuficienteException(
                    "Stock insuficiente para '" + nombre + "': se requieren " + entrada.getValue() + ", hay " + disponible + "."
                );
            }
        }
    }

    public void anular(int idPedido) throws SQLException {
        Pedido pedido = pedidoDAO.buscarPorId(idPedido);
        if (pedido == null)
            throw new IllegalArgumentException("No se encontró el pedido.");
        if (!"PENDIENTE".equals(pedido.getEstado()))
            throw new IllegalArgumentException("Solo se pueden anular pedidos en estado PENDIENTE.");
        pedidoDAO.cambiarEstado(idPedido, "ANULADO");
    }

    public List<Pedido> listarPendientesPorCliente(int idCliente) throws SQLException {
        return pedidoDAO.listarPendientesPorCliente(idCliente);
    }

    public List<PedidoDetalle> listarDetalle(int idPedido) throws SQLException {
        return pedidoDAO.listarDetalle(idPedido);
    }

    public Pedido buscarPorId(int idPedido) throws SQLException {
        return pedidoDAO.buscarPorId(idPedido);
    }

    private PedidoDetalle construirDetalle(int idPedido, ItemVenta item) {
        if (item instanceof ProductoIndividual pi) {
            return new PedidoDetalle(0, idPedido, "PRODUCTO",
                pi.getProducto().getIdProducto(), null,
                pi.getCantidad(), pi.getPrecioUnitario(),
                pi.calcularSubtotal(), pi.calcularPuntos());
        } else {
            MenuCompleto mc = (MenuCompleto) item;
            return new PedidoDetalle(0, idPedido, "MENU",
                null, mc.getMenu().getIdMenu(),
                mc.getCantidad(), mc.getPrecioUnitario(),
                mc.calcularSubtotal(), mc.calcularPuntos());
        }
    }
}

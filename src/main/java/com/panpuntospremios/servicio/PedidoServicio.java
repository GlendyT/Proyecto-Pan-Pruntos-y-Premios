package com.panpuntospremios.servicio;

import com.panpuntospremios.dao.PedidoDAO;
import com.panpuntospremios.dao.ProductoDAO;
import com.panpuntospremios.excepcion.StockInsuficienteException;
import com.panpuntospremios.modelo.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.TreeMap;
import java.util.ArrayList;
import java.math.BigDecimal;
import com.panpuntospremios.dao.VentaDAO;
import com.panpuntospremios.excepcion.ValidacionException;
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

        if (pedido == null) throw new ValidacionException("El pedido es obligatorio.");
        if (!conexion.getAutoCommit()) throw new ValidacionException("Ya existe una transacción en curso.");
        try {
            conexion.setAutoCommit(false);
            VentaDAO flujo = new VentaDAO(conexion);
            flujo.bloquearCliente(pedido.getIdCliente());
            Map<Integer, Menu> menus = new TreeMap<>();
            for (ItemVenta item : items) {
                if (item instanceof MenuCompleto mc) menus.put(mc.getMenu().getIdMenu(), null);
                else if (!(item instanceof ProductoIndividual)) throw new ValidacionException("Ítem no válido.");
            }
            for (Integer id : menus.keySet()) menus.put(id, flujo.bloquearMenu(id));
            List<ItemVenta> actuales = new ArrayList<>();
            for (ItemVenta item : items) {
                ItemVenta actual;
                if (item instanceof ProductoIndividual pi) {
                    Producto producto = productoDAO.buscarPorId(pi.getProducto().getIdProducto());
                    if (producto == null || !producto.isActivo()) throw new ValidacionException("Producto inactivo o inexistente.");
                    actual = new ProductoIndividual(producto, item.getCantidad());
                } else {
                    actual = new MenuCompleto(menus.get(((MenuCompleto)item).getMenu().getIdMenu()), item.getCantidad());
                }
                if (actual.getPrecioUnitario().compareTo(item.getPrecioUnitario()) != 0)
                    throw new ValidacionException("El precio cambió. Vuelve a cargar el catálogo y agrega el ítem.");
                actuales.add(actual);
            }
            items = actuales;
            validarStock(items);
            BigDecimal total = BigDecimal.ZERO;
            int puntos = 0;
            for (ItemVenta item : items) {
                total = total.add(item.calcularSubtotal());
                puntos = Math.addExact(puntos, item.calcularPuntos());
            }
            pedido.setTotal(total);
            pedido.setPuntosGenerados(puntos);

            int idPedido = pedidoDAO.guardar(pedido);
            for (ItemVenta item : items) {
                pedidoDAO.guardarDetalle(construirDetalle(idPedido, item));
            }

            conexion.commit();
            return idPedido;
        } catch (SQLException | StockInsuficienteException | RuntimeException e) {
            try { conexion.rollback(); } catch (SQLException fallo) { e.addSuppressed(fallo); }
            throw e;
        } finally {
            conexion.setAutoCommit(true);
        }
    }

    private void validarStock(List<ItemVenta> items) throws SQLException, StockInsuficienteException {
        // Consolidar cantidades requeridas por producto (incluyendo componentes de menús)
        Map<Integer, Integer> requerido = new TreeMap<>();
        for (ItemVenta item : items) {
            if (item instanceof ProductoIndividual pi) {
                int id = pi.getProducto().getIdProducto();
                requerido.merge(id, pi.getCantidad(), Math::addExact);
            } else if (item instanceof MenuCompleto mc) {
                for (Producto componente : mc.getMenu().getComponentes()) {
                    requerido.merge(componente.getIdProducto(), mc.getCantidad(), Math::addExact);
                }
            }
        }
        Map<Integer, Producto> bloqueados = new TreeMap<>();
        // Orden estable para evitar ciclos de espera entre pedidos concurrentes.
        for (Map.Entry<Integer, Integer> entrada : requerido.entrySet()) {
            Producto producto = productoDAO.bloquearPorId(entrada.getKey());
            if (producto == null || !producto.isActivo())
                throw new ValidacionException("Producto inexistente o inactivo: " + entrada.getKey());
            if (producto.getExistencia() < entrada.getValue()) {
                String nombre = producto != null ? producto.getNombre() : "ID " + entrada.getKey();
                int disponible = producto != null ? producto.getExistencia() : 0;
                throw new StockInsuficienteException(
                    "Stock insuficiente para '" + nombre + "': se requieren " + entrada.getValue() + ", hay " + disponible + "."
                );
            }
            bloqueados.put(entrada.getKey(), producto);
        }
        for (ItemVenta item : items) {
            if (item instanceof ProductoIndividual pi) {
                Producto actual = bloqueados.get(pi.getProducto().getIdProducto());
                if (actual.getPrecio().compareTo(item.getPrecioUnitario()) != 0 ||
                    !actual.getCategoria().equals(pi.getProducto().getCategoria()))
                    throw new ValidacionException("El producto cambió. Actualiza el catálogo.");
            }
        }
    }

    public void anular(int idPedido) throws SQLException {
        com.panpuntospremios.util.Transaccion.ejecutar(conexion, () -> {
            Pedido pedido = new VentaDAO(conexion).bloquearPedido(idPedido);
            if (!"PENDIENTE".equals(pedido.getEstado()))
                throw new ValidacionException("Solo se pueden anular pedidos en estado PENDIENTE.");
            pedidoDAO.cambiarEstado(idPedido, "ANULADO");
            return null;
        });
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

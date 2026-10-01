package com.panpuntospremios.servicio;

import com.panpuntospremios.dao.*;
import com.panpuntospremios.enums.MetodoPago;
import com.panpuntospremios.excepcion.*;
import com.panpuntospremios.modelo.*;
import com.panpuntospremios.util.Transaccion;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.*;

public class VentaServicio {
    private final Connection conexion;
    private final PedidoDAO pedidos;
    private final ProductoDAO productos;
    private final MovimientoInventarioDAO movimientos;
    private final VentaDAO ventas;

    public VentaServicio(Connection conexion) {
        this.conexion = conexion;
        pedidos = new PedidoDAOImpl(conexion);
        productos = new ProductoDAOImpl(conexion);
        movimientos = new MovimientoInventarioDAOImpl(conexion);
        ventas = new VentaDAO(conexion);
    }

    public ComprobanteVenta cobrar(int idPedido, MetodoPago metodo, BigDecimal recibido, boolean tarjetaAprobada)
            throws SQLException, StockInsuficienteException, PagoRechazadoException {
        if (metodo == null) throw new ValidacionException("Selecciona un método de pago.");
        if (!conexion.getAutoCommit()) throw new ValidacionException("Ya existe una transacción en curso.");
        conexion.setAutoCommit(false);
        try {
            Pedido pedido = ventas.bloquearPedido(idPedido);
            if (!"PENDIENTE".equals(pedido.getEstado()))
                throw new ValidacionException("Solo se puede cobrar un pedido PENDIENTE; no se permiten cobros duplicados.");
            List<PedidoDetalle> detalle = pedidos.listarDetalle(idPedido);
            validarImportes(pedido, detalle);
            String referencia;
            if (metodo == MetodoPago.EFECTIVO) {
                if (recibido == null || recibido.signum() < 0 || recibido.compareTo(new BigDecimal("99999999.99")) > 0)
                    throw new ValidacionException("El efectivo recibido debe ser un importe válido de hasta 8 enteros y 2 decimales.");
                try { recibido = recibido.setScale(2, RoundingMode.UNNECESSARY); }
                catch (ArithmeticException e) { throw new ValidacionException("El efectivo admite como máximo dos decimales."); }
                if (recibido.compareTo(pedido.getTotal()) < 0)
                    throw new PagoRechazadoException("El efectivo recibido no cubre el total del pedido.");
                referencia = "RECIBIDO:" + recibido.toPlainString();
            } else {
                if (!tarjetaAprobada) throw new PagoRechazadoException("Pago con tarjeta rechazado por el simulador.");
                referencia = "SIM-" + UUID.randomUUID();
            }
            int saldo = ventas.bloquearCliente(pedido.getIdCliente());
            Map<Integer, Menu> menus = new TreeMap<>();
            for (PedidoDetalle d : detalle) if ("MENU".equals(d.getTipoItem())) menus.put(d.getIdMenu(), null);
            for (Integer id : menus.keySet()) menus.put(id, ventas.bloquearMenu(id));
            Map<Integer, Integer> requerido = new TreeMap<>();
            for (PedidoDetalle d : detalle) {
                if ("PRODUCTO".equals(d.getTipoItem())) requerido.merge(d.getIdProducto(), d.getCantidad(), Math::addExact);
                else for (Producto p : menus.get(d.getIdMenu()).getComponentes())
                    requerido.merge(p.getIdProducto(), d.getCantidad(), Math::addExact);
            }
            // Bloqueos por ID creciente: dos ventas comparten el mismo orden.
            for (var entrada : requerido.entrySet()) {
                Producto producto = productos.bloquearPorId(entrada.getKey());
                if (producto == null || !producto.isActivo()) throw new ValidacionException("Producto inexistente o inactivo: " + entrada.getKey());
                if (producto.getExistencia() < entrada.getValue())
                    throw new StockInsuficienteException("Stock insuficiente para " + producto.getNombre() + ".");
                int restante = producto.getExistencia() - entrada.getValue();
                productos.actualizarExistencia(producto.getIdProducto(), restante);
                movimientos.registrar(new MovimientoInventario(0, producto.getIdProducto(), null, "VENTA", entrada.getValue(), restante));
            }
            ventas.acreditar(pedido.getIdCliente(), saldo, pedido.getPuntosGenerados(), idPedido);
            ventas.pagar(idPedido, metodo.name(), referencia);
            ComprobanteVenta comprobante = comprobante(idPedido);
            conexion.commit();
            return comprobante;
        } catch (SQLException | StockInsuficienteException | PagoRechazadoException | RuntimeException e) {
            try { conexion.rollback(); } catch (SQLException fallo) { e.addSuppressed(fallo); }
            throw e;
        } finally {
            conexion.setAutoCommit(true);
        }
    }

    private void validarImportes(Pedido pedido, List<PedidoDetalle> detalle) {
        if (detalle.isEmpty()) throw new ValidacionException("El pedido no tiene detalle.");
        BigDecimal total = BigDecimal.ZERO; int puntos = 0;
        for (PedidoDetalle d : detalle) {
            boolean producto = "PRODUCTO".equals(d.getTipoItem()) && d.getIdProducto() != null && d.getIdMenu() == null;
            boolean menu = "MENU".equals(d.getTipoItem()) && d.getIdMenu() != null && d.getIdProducto() == null;
            if ((!producto && !menu) || d.getCantidad() <= 0 || d.getPrecioUnitario().signum() < 0 || d.getPuntos() < 0 ||
                d.getSubtotal().compareTo(d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad()))) != 0)
                throw new ValidacionException("El detalle guardado es inconsistente.");
            total = total.add(d.getSubtotal()); puntos = Math.addExact(puntos, d.getPuntos());
        }
        if (total.compareTo(pedido.getTotal()) != 0 || puntos != pedido.getPuntosGenerados())
            throw new ValidacionException("Los importes o puntos guardados no coinciden con el detalle.");
    }

    public ComprobanteVenta comprobante(int idPedido) throws SQLException {
        Pedido pedido = pedidos.buscarPorId(idPedido);
        if (pedido == null || !("PAGADO".equals(pedido.getEstado()) || "ENTREGADO".equals(pedido.getEstado())))
            throw new ValidacionException("El comprobante está disponible para pedidos pagados o entregados.");
        return new ComprobanteVenta(pedido, pedidos.listarDetalle(idPedido));
    }

    public void entregar(int idPedido) throws SQLException {
        Transaccion.ejecutar(conexion, () -> {
            Pedido pedido = ventas.bloquearPedido(idPedido);
            if (!"PAGADO".equals(pedido.getEstado())) throw new ValidacionException("Solo se pueden entregar pedidos PAGADOS.");
            pedidos.cambiarEstado(idPedido, "ENTREGADO"); return null;
        });
    }
}

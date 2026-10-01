package com.panpuntospremios.modelo;

import java.math.BigDecimal;
import java.util.List;

/** Datos recuperados del pedido persistido; no consulta precios del catálogo. */
public record ComprobanteVenta(Pedido pedido, List<PedidoDetalle> detalle) {
    public ComprobanteVenta { detalle = List.copyOf(detalle); }
    public BigDecimal recibido() {
        String referencia = pedido.getReferenciaPago();
        return "EFECTIVO".equals(pedido.getMetodoPago()) && referencia != null && referencia.startsWith("RECIBIDO:")
            ? new BigDecimal(referencia.substring("RECIBIDO:".length())) : null;
    }
    public BigDecimal cambio() { return recibido() == null ? null : recibido().subtract(pedido.getTotal()); }
    public String texto() {
        StringBuilder texto = new StringBuilder("PAN, PUNTOS Y PREMIOS\n");
        texto.append("Pedido #").append(pedido.getIdPedido()).append(" | Cliente #").append(pedido.getIdCliente())
            .append("\nFecha del pedido: ").append(pedido.getFecha()).append("\nEstado: ").append(pedido.getEstado()).append("\n\n");
        for (PedidoDetalle d : detalle) {
            texto.append(d.getTipoItem()).append(" #").append(d.getIdProducto() != null ? d.getIdProducto() : d.getIdMenu())
                .append(" | ").append(d.getCantidad()).append(" x Q").append(d.getPrecioUnitario())
                .append(" = Q").append(d.getSubtotal()).append("\n");
        }
        texto.append("\nTotal: Q").append(pedido.getTotal()).append("\nPuntos acreditados: ").append(pedido.getPuntosGenerados())
            .append("\nPago: ").append(pedido.getMetodoPago()).append("\nReferencia: ").append(pedido.getReferenciaPago());
        if (recibido() != null) texto.append("\nRecibido: Q").append(recibido()).append("\nCambio: Q").append(cambio());
        return texto.toString();
    }
}

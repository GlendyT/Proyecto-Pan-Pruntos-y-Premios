package com.panpuntospremios.modelo;

import java.math.BigDecimal;

public class PedidoDetalle {
    private int idDetalle;
    private int idPedido;
    private String tipoItem;
    private Integer idProducto;
    private Integer idMenu;
    private int cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
    private int puntos;

    public PedidoDetalle(int idDetalle, int idPedido, String tipoItem, Integer idProducto, Integer idMenu, int cantidad, BigDecimal precioUnitario, BigDecimal subtotal, int puntos) {
        this.idDetalle = idDetalle;
        this.idPedido = idPedido;
        this.tipoItem = tipoItem;
        this.idProducto = idProducto;
        this.idMenu = idMenu;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.subtotal = subtotal;
        this.puntos = puntos;
    }

    public int getIdDetalle() { return idDetalle; }
    public int getIdPedido() { return idPedido; }
    public String getTipoItem() { return tipoItem; }
    public Integer getIdProducto() { return idProducto; }
    public Integer getIdMenu() { return idMenu; }
    public int getCantidad() { return cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public BigDecimal getSubtotal() { return subtotal; }
    public int getPuntos() { return puntos; }
}

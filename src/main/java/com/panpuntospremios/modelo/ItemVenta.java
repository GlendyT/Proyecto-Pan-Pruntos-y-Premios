package com.panpuntospremios.modelo;

import java.math.BigDecimal;

public abstract class ItemVenta {
    private int cantidad;
    private BigDecimal precioUnitario;

    public ItemVenta(int cantidad, BigDecimal precioUnitario) {
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal calcularSubtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    public abstract int calcularPuntos();

    public int getCantidad() { return cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
}

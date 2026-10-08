package com.panpuntospremios.modelo;

import com.panpuntospremios.excepcion.ValidacionException;

import java.math.BigDecimal;

public abstract class ItemVenta {
    private int cantidad;
    private BigDecimal precioUnitario;

    public ItemVenta(int cantidad, BigDecimal precioUnitario) {
        if (cantidad <= 0) {
            throw new ValidacionException("La cantidad debe ser mayor que cero.");
        }
        if (precioUnitario == null) {
            throw new ValidacionException("El precio unitario es obligatorio.");
        }
        if (precioUnitario.signum() < 0) {
            throw new ValidacionException("El precio unitario no puede ser negativo.");
        }
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

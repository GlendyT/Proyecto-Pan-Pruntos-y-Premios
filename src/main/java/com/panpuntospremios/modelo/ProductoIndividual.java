package com.panpuntospremios.modelo;

public class ProductoIndividual extends ItemVenta {
    private Producto producto;

    public ProductoIndividual(Producto producto, int cantidad) {
        super(cantidad, producto.getPrecio());
        this.producto = producto;
    }

    @Override
    public int calcularPuntos() {
        if ("SANDWICH".equals(producto.getCategoria())) {
            return Math.multiplyExact(getCantidad(), 2);
        }
        return 0;
    }

    public Producto getProducto() { return producto; }
}

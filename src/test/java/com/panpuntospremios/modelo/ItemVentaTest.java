package com.panpuntospremios.modelo;

import com.panpuntospremios.excepcion.ValidacionException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItemVentaTest {

    @Test
    void calculaTotalYPuntosDePedidoMixto() {
        Producto sandwich = producto("SAN001", "SANDWICH", "25.00");
        Producto bebida = producto("BEB001", "BEBIDA", "8.00");
        Producto acompanamiento = producto("ACO001", "ACOMPANAMIENTO", "10.00");
        Menu menu = new Menu(1, "MEN001", "Menú completo", new BigDecimal("37.00"), true);

        List<ItemVenta> items = List.of(
            new ProductoIndividual(sandwich, 2),
            new ProductoIndividual(bebida, 1),
            new ProductoIndividual(acompanamiento, 1),
            new MenuCompleto(menu, 2)
        );

        BigDecimal total = items.stream()
            .map(ItemVenta::calcularSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        int puntos = items.stream().mapToInt(ItemVenta::calcularPuntos).sum();

        assertEquals(new BigDecimal("142.00"), total);
        assertEquals(20, puntos);
    }

    @Test
    void conservaLaPrecisionDecimalAlCalcularSubtotal() {
        Producto sandwich = producto("SAN002", "SANDWICH", "10.25");

        ItemVenta item = new ProductoIndividual(sandwich, 3);

        assertEquals(new BigDecimal("30.75"), item.calcularSubtotal());
        assertEquals(6, item.calcularPuntos());
    }

    @Test
    void bebidasYAcompanamientosNoGeneranPuntos() {
        ItemVenta bebida = new ProductoIndividual(producto("BEB001", "BEBIDA", "8.00"), 3);
        ItemVenta acompanamiento = new ProductoIndividual(producto("ACO001", "ACOMPANAMIENTO", "10.00"), 4);

        assertEquals(0, bebida.calcularPuntos());
        assertEquals(0, acompanamiento.calcularPuntos());
    }

    @Test
    void rechazaCantidadCeroONegativa() {
        Producto producto = producto("SAN001", "SANDWICH", "25.00");

        assertThrows(ValidacionException.class, () -> new ProductoIndividual(producto, 0));
        assertThrows(ValidacionException.class, () -> new ProductoIndividual(producto, -1));
    }

    @Test
    void rechazaPrecioNuloONegativo() {
        Producto sinPrecio = producto("SAN001", "SANDWICH", null);
        Producto precioNegativo = producto("SAN002", "SANDWICH", "-0.01");

        assertThrows(ValidacionException.class, () -> new ProductoIndividual(sinPrecio, 1));
        assertThrows(ValidacionException.class, () -> new ProductoIndividual(precioNegativo, 1));
    }

    @Test
    void calculaUnLimiteAltoSinPerderPrecision() {
        Producto sandwich = producto("SAN001", "SANDWICH", "999.99");

        ItemVenta item = new ProductoIndividual(sandwich, 1_000_000);

        assertEquals(new BigDecimal("999990000.00"), item.calcularSubtotal());
        assertEquals(2_000_000, item.calcularPuntos());
    }

    private Producto producto(String codigo, String categoria, String precio) {
        return new Producto(
            1,
            codigo,
            categoria,
            "Producto de prueba",
            precio == null ? null : new BigDecimal(precio),
            10,
            true
        );
    }
}

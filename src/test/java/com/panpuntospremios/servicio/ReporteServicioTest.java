package com.panpuntospremios.servicio;

import com.panpuntospremios.dao.*;
import com.panpuntospremios.enums.MetodoPago;
import com.panpuntospremios.excepcion.ValidacionException;
import com.panpuntospremios.modelo.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ReporteServicioTest extends BasePedidoTest {
    ReporteServicio reportes() { return new ReporteServicio(new ReporteDAOImpl(c)); }
    int ventaPagada() throws Exception {
        Producto sandwich = new ProductoDAOImpl(c).buscarPorCodigo("SAN001");
        ItemVenta item = new ProductoIndividual(sandwich, 1);
        Pedido pedido = new Pedido(0, 1, null, "PENDIENTE", item.calcularSubtotal(), item.calcularPuntos(), null, null);
        int id = servicio(c).crearPedido(pedido, List.of(item));
        new VentaServicio(c).cobrar(id, MetodoPago.TARJETA, null, true);
        return id;
    }
    @Test void reporteVentasIncluyePagadasYRespetaEstadoYFechas() throws Exception {
        int id = ventaPagada();
        ReporteDatos todos = reportes().consultar(ReporteServicio.Tipo.VENTAS,
            LocalDate.now(), LocalDate.now(), "TODOS");
        assertEquals(1, todos.filas().size());
        assertEquals(id, ((Number)todos.filas().get(0).get(0)).intValue());
        assertEquals("PAGADO", todos.filas().get(0).get(4));
        assertEquals(0, reportes().consultar(ReporteServicio.Tipo.VENTAS,
            LocalDate.now().plusDays(1), null, "TODOS").filas().size());
        assertEquals(1, reportes().consultar(ReporteServicio.Tipo.VENTAS,
            null, null, "PAGADO").filas().size());
        assertEquals(0, reportes().consultar(ReporteServicio.Tipo.VENTAS,
            null, null, "ENTREGADO").filas().size());
    }
    @Test void inventarioMuestraExistenciasYPermiteFiltrarActivos() throws Exception {
        ReporteDatos activos = reportes().consultar(ReporteServicio.Tipo.INVENTARIO, null, null, "ACTIVOS");
        assertEquals(4, activos.filas().size());
        ejecutar("UPDATE producto SET activo = 'N' WHERE codigo = 'SAN002'");
        ReporteDatos inactivos = reportes().consultar(ReporteServicio.Tipo.INVENTARIO, null, null, "INACTIVOS");
        assertEquals(1, inactivos.filas().size());
        assertEquals("SAN002", inactivos.filas().get(0).get(1));
    }

    @Test void reportesReflejanFlujoDeClienteAbastecimientoVentaCobroCanje() throws Exception {
        String identificador = "RPT" + java.util.UUID.randomUUID().toString().substring(0, 8);
        new ClienteDAOImpl(c).registrar(new Cliente(0, identificador, "Cliente Reportes", "55551234",
            "reportes@example.com", 0, true, null));
        int idCliente = new ClienteDAOImpl(c).buscarPorIdentificador(identificador).getIdCliente();

        new InventarioServicio(c, new ProductoDAOImpl(c), new MovimientoInventarioDAOImpl(c)).abastecer(1, 2);
        Producto sandwich = new ProductoDAOImpl(c).buscarPorId(1);
        ItemVenta item = new ProductoIndividual(sandwich, 5);
        Pedido pedido = new Pedido(0, idCliente, null, "PENDIENTE", BigDecimal.ZERO, 0, null, null);
        int idPedido = new PedidoServicio(new PedidoDAOImpl(c), new ProductoDAOImpl(c), c)
            .crearPedido(pedido, List.of(item));
        new VentaServicio(c).cobrar(idPedido, MetodoPago.TARJETA, null, true);
        new PuntosYCanjesServicio(c).canjear(idCliente, 1);

        ReporteDatos ventas = reportes().consultar(ReporteServicio.Tipo.VENTAS, null, null, "PAGADO");
        assertTrue(ventas.filas().stream().anyMatch(f -> ((Number)f.get(0)).intValue() == idPedido));
        ReporteDatos inventario = reportes().consultar(ReporteServicio.Tipo.INVENTARIO, null, null, "TODOS");
        assertEquals(22, inventario.filas().stream().filter(f -> "SAN001".equals(f.get(1)))
            .mapToInt(f -> ((Number)f.get(5)).intValue()).findFirst().orElseThrow());
        assertEquals(49, inventario.filas().stream().filter(f -> "BEB001".equals(f.get(1)))
            .mapToInt(f -> ((Number)f.get(5)).intValue()).findFirst().orElseThrow());
        var tiposInventario = reportes().consultar(ReporteServicio.Tipo.MOVIMIENTOS_INVENTARIO,
            null, null, "TODOS").filas().stream().map(f -> f.get(2).toString()).toList();
        assertTrue(tiposInventario.containsAll(List.of("ABASTECIMIENTO", "VENTA", "CANJE")));
        var tiposPuntos = reportes().consultar(ReporteServicio.Tipo.MOVIMIENTOS_PUNTOS,
            null, null, "TODOS").filas().stream().map(f -> f.get(4).toString()).toList();
        assertTrue(tiposPuntos.containsAll(List.of("ACUMULACION", "CANJE")));
        // El pedido acredita los puntos y el canje consume ese saldo.
        assertEquals(0, numero("SELECT saldo_puntos FROM cliente WHERE id_cliente = " + idCliente));
    }
    @Test void movimientosSeFiltranPorFechasTipoYSeOrdenan() throws Exception {
        ejecutar("INSERT INTO movimiento_inventario (id_producto, tipo, cantidad, existencia_resultante) VALUES (1, 'ABASTECIMIENTO', 2, 27)");
        ejecutar("INSERT INTO movimiento_inventario (id_producto, tipo, cantidad, existencia_resultante) VALUES (1, 'VENTA', 1, 26)");
        ejecutar("INSERT INTO movimiento_puntos (id_cliente, tipo, puntos, referencia) VALUES (1, 'ACUMULACION', 2, 'PEDIDO:1')");
        assertEquals(1, reportes().consultar(ReporteServicio.Tipo.MOVIMIENTOS_INVENTARIO,
            LocalDate.now(), LocalDate.now(), "ABASTECIMIENTO").filas().size());
        assertEquals(0, reportes().consultar(ReporteServicio.Tipo.MOVIMIENTOS_INVENTARIO,
            LocalDate.now().plusDays(1), null, "TODOS").filas().size());
        assertEquals(1, reportes().consultar(ReporteServicio.Tipo.MOVIMIENTOS_PUNTOS,
            null, null, "ACUMULACION").filas().size());
    }
    @Test void rechazaTipoFiltroYRangoInvalidos() {
        assertThrows(ValidacionException.class, () -> reportes().consultar(null, null, null, "TODOS"));
        assertThrows(ValidacionException.class, () -> reportes().consultar(ReporteServicio.Tipo.VENTAS,
            LocalDate.now(), LocalDate.now().minusDays(1), "TODOS"));
        assertThrows(ValidacionException.class, () -> reportes().consultar(ReporteServicio.Tipo.VENTAS,
            null, null, "ANULADO"));
    }
}

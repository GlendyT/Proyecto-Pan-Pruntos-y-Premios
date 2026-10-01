package com.panpuntospremios.servicio;

import com.panpuntospremios.dao.*;
import com.panpuntospremios.modelo.*;
import com.panpuntospremios.enums.MetodoPago;
import com.panpuntospremios.excepcion.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class VentaServicioTest extends BasePedidoTest {
    ComprobanteVenta efectivo(int id) throws Exception {
        return new VentaServicio(c).cobrar(id, MetodoPago.EFECTIVO, new BigDecimal("200"), false);
    }
    void sinVenta(int id) throws Exception {
        assertEquals("PENDIENTE", servicio(c).buscarPorId(id).getEstado());
        assertNull(servicio(c).buscarPorId(id).getMetodoPago());
        assertNull(servicio(c).buscarPorId(id).getReferenciaPago());
        assertEquals(25, numero("SELECT existencia FROM producto WHERE id_producto = 1"));
        assertEquals(50, numero("SELECT existencia FROM producto WHERE id_producto = 3"));
        assertEquals(35, numero("SELECT existencia FROM producto WHERE id_producto = 4"));
        assertEquals(0, numero("SELECT saldo_puntos FROM cliente WHERE id_cliente = 1"));
        assertEquals(0, numero("SELECT COUNT(*) FROM movimiento_inventario"));
        assertEquals(0, numero("SELECT COUNT(*) FROM movimiento_puntos"));
    }
    @Test void cobraMixtoEnEfectivoYRecuperaComprobanteConPreciosGuardados() throws Exception {
        int id = crearMixto();
        ejecutar("UPDATE producto SET precio = 99 WHERE id_producto = 1");
        ejecutar("UPDATE menu SET precio = 100 WHERE id_menu = 1");
        ComprobanteVenta venta = efectivo(id);
        assertEquals(new BigDecimal("39.00"), venta.cambio());
        assertEquals(20, numero("SELECT existencia FROM producto WHERE id_producto = 1"));
        assertEquals(47, numero("SELECT existencia FROM producto WHERE id_producto = 3"));
        assertEquals(32, numero("SELECT existencia FROM producto WHERE id_producto = 4"));
        assertEquals(28, numero("SELECT saldo_puntos FROM cliente WHERE id_cliente = 1"));
        assertEquals(3, numero("SELECT COUNT(*) FROM movimiento_inventario WHERE tipo = 'VENTA'"));
        assertEquals(5, numero("SELECT cantidad FROM movimiento_inventario WHERE id_producto = 1"));
        assertEquals(20, numero("SELECT existencia_resultante FROM movimiento_inventario WHERE id_producto = 1"));
        assertEquals(28, numero("SELECT puntos FROM movimiento_puntos WHERE referencia = 'PEDIDO:" + id + "'"));
        c.close(); c = DriverManager.getConnection(url);
        ComprobanteVenta recuperado = new VentaServicio(c).comprobante(id);
        assertEquals(venta.texto(), recuperado.texto());
        assertEquals("PAGADO", recuperado.pedido().getEstado());
        assertEquals(new BigDecimal("161.00"), recuperado.pedido().getTotal());
        assertEquals(new BigDecimal("200.00"), recuperado.recibido());
    }
    @Test void tarjetaAprobadaTieneReferenciaPersistida() throws Exception {
        int id = crearMixto();
        var venta = new VentaServicio(c).cobrar(id, MetodoPago.TARJETA, null, true);
        assertEquals("TARJETA", venta.pedido().getMetodoPago());
        assertTrue(venta.pedido().getReferenciaPago().startsWith("SIM-"));
        assertNull(venta.cambio());
    }
    @Test void tarjetaRechazadaNoModificaNada() throws Exception {
        int id = crearMixto();
        assertThrows(PagoRechazadoException.class, () -> new VentaServicio(c).cobrar(id, MetodoPago.TARJETA, null, false));
        sinVenta(id); assertTrue(c.getAutoCommit());
    }
    @Test void efectivoInsuficienteONoValidoNoModificaNada() throws Exception {
        int id = crearMixto(); VentaServicio venta = new VentaServicio(c);
        assertThrows(PagoRechazadoException.class, () -> venta.cobrar(id, MetodoPago.EFECTIVO, BigDecimal.ONE, false));
        assertThrows(ValidacionException.class, () -> venta.cobrar(id, MetodoPago.EFECTIVO, new BigDecimal("200.001"), false));
        assertThrows(ValidacionException.class, () -> venta.cobrar(id, MetodoPago.EFECTIVO, null, false));
        assertThrows(ValidacionException.class, () -> venta.cobrar(id, MetodoPago.EFECTIVO, new BigDecimal("-1"), false));
        assertThrows(ValidacionException.class, () -> venta.cobrar(id, null, null, false));
        sinVenta(id);
    }
    @Test void admiteEfectivoExacto() throws Exception {
        var venta = new VentaServicio(c).cobrar(crearMixto(), MetodoPago.EFECTIVO, new BigDecimal("161"), false);
        assertEquals(new BigDecimal("0.00"), venta.cambio());
    }
    @Test void stockInsuficientePosteriorRevierteProductosYaDescontados() throws Exception {
        int id = crearMixto(); ejecutar("UPDATE producto SET existencia = 0 WHERE id_producto = 4");
        assertThrows(StockInsuficienteException.class, () -> efectivo(id));
        assertEquals(20 + 5, numero("SELECT existencia FROM producto WHERE id_producto = 1"));
        assertEquals(50, numero("SELECT existencia FROM producto WHERE id_producto = 3"));
        assertEquals(0, numero("SELECT COUNT(*) FROM movimiento_inventario"));
        assertEquals(0, numero("SELECT COUNT(*) FROM movimiento_puntos"));
        assertEquals("PENDIENTE", servicio(c).buscarPorId(id).getEstado());
    }
    @Test void falloEnMovimientoPuntosRevierteInventarioYSaldo() throws Exception {
        int id = crearMixto();
        ejecutar("ALTER TABLE movimiento_puntos ADD CONSTRAINT fallo_puntos CHECK (puntos < 0)");
        assertThrows(SQLException.class, () -> efectivo(id)); sinVenta(id);
    }
    @Test void falloAlMarcarPagadoRevierteTodaLaVenta() throws Exception {
        int id = crearMixto();
        ejecutar("ALTER TABLE pedido ADD CONSTRAINT fallo_pago CHECK (estado <> 'PAGADO')");
        assertThrows(SQLException.class, () -> efectivo(id)); sinVenta(id);
    }
    @Test void falloDeEjecucionPorDesbordamientoSaldoRevierteInventario() throws Exception {
        int id = crearMixto(); ejecutar("UPDATE cliente SET saldo_puntos = 2147483647 WHERE id_cliente = 1");
        assertThrows(ArithmeticException.class, () -> efectivo(id));
        assertEquals(25, numero("SELECT existencia FROM producto WHERE id_producto = 1"));
        assertEquals(0, numero("SELECT COUNT(*) FROM movimiento_inventario"));
        assertEquals(0, numero("SELECT COUNT(*) FROM movimiento_puntos"));
        assertEquals(Integer.MAX_VALUE, numero("SELECT saldo_puntos FROM cliente WHERE id_cliente = 1"));
    }
    @Test void cobroRepetidoYAnulacionTrasPagoNoDuplicanEfectos() throws Exception {
        int id = crearMixto(); efectivo(id);
        assertThrows(ValidacionException.class, () -> efectivo(id));
        assertThrows(IllegalArgumentException.class, () -> servicio(c).anular(id));
        assertEquals(1, numero("SELECT COUNT(*) FROM movimiento_puntos"));
        assertEquals(20, numero("SELECT existencia FROM producto WHERE id_producto = 1"));
    }
    @Test void validaEstadosDeEntregaYComprobante() throws Exception {
        int id = crearMixto(); VentaServicio venta = new VentaServicio(c);
        assertThrows(ValidacionException.class, () -> venta.entregar(id));
        assertThrows(ValidacionException.class, () -> venta.comprobante(id));
        efectivo(id); venta.entregar(id);
        assertEquals("ENTREGADO", venta.comprobante(id).pedido().getEstado());
        assertThrows(ValidacionException.class, () -> venta.entregar(id));
        assertThrows(ValidacionException.class, () -> efectivo(id));
        assertEquals(1, numero("SELECT COUNT(*) FROM movimiento_puntos"));
    }
    @Test void noCobraAnuladoNiClienteInactivoNiCabeceraInconsistente() throws Exception {
        int id = crearMixto(); ejecutar("UPDATE cliente SET activo = 'N' WHERE id_cliente = 1");
        assertThrows(ValidacionException.class, () -> efectivo(id)); sinVenta(id);
        ejecutar("UPDATE cliente SET activo = 'S' WHERE id_cliente = 1");
        ejecutar("UPDATE pedido SET total = 1 WHERE id_pedido = " + id);
        assertThrows(ValidacionException.class, () -> efectivo(id)); sinVenta(id);
        servicio(c).anular(id);
        assertThrows(ValidacionException.class, () -> efectivo(id));
    }
    @FunctionalInterface interface Accion { void ejecutar(Connection conexion) throws Exception; }
    List<Boolean> concurrentes(Accion primera, Accion segunda) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch listas = new CountDownLatch(2), inicio = new CountDownLatch(1);
        try {
            List<Future<Boolean>> tareas = new ArrayList<>();
            for (Accion accion : List.of(primera, segunda)) tareas.add(pool.submit(() -> {
                try (Connection otra = DriverManager.getConnection(url)) {
                    listas.countDown();
                    if (!inicio.await(5, TimeUnit.SECONDS)) throw new AssertionError("No inició la prueba");
                    try { accion.ejecutar(otra); return true; }
                    catch (ValidacionException | StockInsuficienteException e) { return false; }
                }
            }));
            assertTrue(listas.await(5, TimeUnit.SECONDS)); inicio.countDown();
            return List.of(tareas.get(0).get(10, TimeUnit.SECONDS), tareas.get(1).get(10, TimeUnit.SECONDS));
        } finally { inicio.countDown(); pool.shutdownNow(); assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS)); }
    }
    void pagar(Connection otra, int id) throws Exception { new VentaServicio(otra).cobrar(id, MetodoPago.TARJETA, null, true); }
    @Test void dosCobrosSimultaneosDelMismoPedidoSoloCobranUnaVez() throws Exception {
        int id = crearMixto();
        var resultados = concurrentes(otra -> pagar(otra, id), otra -> pagar(otra, id));
        assertEquals(1L, resultados.stream().filter(Boolean::booleanValue).count());
        assertEquals(1, numero("SELECT COUNT(*) FROM movimiento_puntos"));
        assertEquals(28, numero("SELECT saldo_puntos FROM cliente WHERE id_cliente = 1"));
    }
    @Test void dosClientesCompitenPorStockSinExistenciasNegativas() throws Exception {
        int primero = crearMixto(); Pedido segundo = cabecera();
        segundo = new Pedido(0, 2, null, "PENDIENTE", BigDecimal.ZERO, 0, null, null);
        int otro = servicio(c).crearPedido(segundo, mixto());
        ejecutar("UPDATE producto SET existencia = 5 WHERE id_producto = 1");
        var resultados = concurrentes(conexion -> pagar(conexion, primero), conexion -> pagar(conexion, otro));
        assertEquals(1L, resultados.stream().filter(Boolean::booleanValue).count());
        assertEquals(0, numero("SELECT existencia FROM producto WHERE id_producto = 1"));
        assertEquals(28, numero("SELECT SUM(saldo_puntos) FROM cliente"));
        assertEquals(1, numero("SELECT COUNT(*) FROM movimiento_puntos"));
    }
    @Test void ventasConcurrentesDelMismoClienteAcumulanAmbosSaldos() throws Exception {
        int primero = crearMixto(), segundo = crearMixto();
        assertEquals(List.of(true, true), concurrentes(otra -> pagar(otra, primero), otra -> pagar(otra, segundo)));
        assertEquals(56, numero("SELECT saldo_puntos FROM cliente WHERE id_cliente = 1"));
        assertEquals(15, numero("SELECT existencia FROM producto WHERE id_producto = 1"));
    }
    @Test void anularYCobrarSimultaneamenteNoSobrescribeVenta() throws Exception {
        int id = crearMixto();
        var resultados = concurrentes(otra -> pagar(otra, id), otra -> servicio(otra).anular(id));
        assertEquals(1L, resultados.stream().filter(Boolean::booleanValue).count());
        String estado = servicio(c).buscarPorId(id).getEstado();
        assertTrue(Set.of("PAGADO", "ANULADO").contains(estado));
        assertEquals("PAGADO".equals(estado) ? 1 : 0, numero("SELECT COUNT(*) FROM movimiento_puntos"));
        assertEquals("PAGADO".equals(estado) ? 20 : 25, numero("SELECT existencia FROM producto WHERE id_producto = 1"));
    }
}

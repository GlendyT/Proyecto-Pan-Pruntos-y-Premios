package com.panpuntospremios.servicio;

import com.panpuntospremios.excepcion.*;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class PuntosYCanjesServicioTest extends BasePedidoTest {

    PuntosYCanjesServicio servicioPuntos() { return new PuntosYCanjesServicio(c); }

    void darPuntos(int puntos) throws Exception {
        ejecutar("UPDATE cliente SET saldo_puntos = " + puntos + " WHERE id_cliente = 1");
    }

    // id_recompensa 1 = Bebida gratuita (10 pts, BEB001)
    // id_recompensa 4 = Menu completo gratuito (50 pts, MEN001)

    @Test
    void canjeExitosoDeProductoDescuentaPuntosEInventario() throws Exception {
        darPuntos(20);
        servicioPuntos().canjear(1, 1);
        assertEquals(10, numero("SELECT saldo_puntos FROM cliente WHERE id_cliente = 1"));
        assertEquals(49, numero("SELECT existencia FROM producto WHERE codigo = 'BEB001'"));
        assertEquals(1, numero("SELECT COUNT(*) FROM canje WHERE id_cliente = 1"));
        assertEquals(1, numero("SELECT COUNT(*) FROM movimiento_inventario WHERE tipo = 'CANJE'"));
        assertEquals(1, numero("SELECT COUNT(*) FROM movimiento_puntos WHERE tipo = 'CANJE'"));
    }

    @Test
    void canjeExitosoDeMenuDescuentaLosTresComponentes() throws Exception {
        darPuntos(60);
        servicioPuntos().canjear(1, 4);
        assertEquals(10, numero("SELECT saldo_puntos FROM cliente WHERE id_cliente = 1"));
        assertEquals(24, numero("SELECT existencia FROM producto WHERE codigo = 'SAN001'"));
        assertEquals(49, numero("SELECT existencia FROM producto WHERE codigo = 'BEB001'"));
        assertEquals(34, numero("SELECT existencia FROM producto WHERE codigo = 'ACO001'"));
        assertEquals(3, numero("SELECT COUNT(*) FROM movimiento_inventario WHERE tipo = 'CANJE'"));
        assertEquals(1, numero("SELECT COUNT(*) FROM canje WHERE id_cliente = 1"));
    }

    @Test
    void puntosInsuficientesNoModificaNada() throws Exception {
        darPuntos(5);
        assertThrows(PuntosInsuficientesException.class, () -> servicioPuntos().canjear(1, 1));
        assertEquals(5, numero("SELECT saldo_puntos FROM cliente WHERE id_cliente = 1"));
        assertEquals(50, numero("SELECT existencia FROM producto WHERE codigo = 'BEB001'"));
        assertEquals(0, numero("SELECT COUNT(*) FROM canje"));
        assertEquals(0, numero("SELECT COUNT(*) FROM movimiento_inventario"));
        assertEquals(0, numero("SELECT COUNT(*) FROM movimiento_puntos"));
        assertTrue(c.getAutoCommit());
    }

    @Test
    void stockInsuficienteRevierteTransaccionCompleta() throws Exception {
        darPuntos(100);
        ejecutar("UPDATE producto SET existencia = 0 WHERE codigo = 'BEB001'");
        assertThrows(StockInsuficienteException.class, () -> servicioPuntos().canjear(1, 1));
        assertEquals(100, numero("SELECT saldo_puntos FROM cliente WHERE id_cliente = 1"));
        assertEquals(0, numero("SELECT COUNT(*) FROM canje"));
        assertEquals(0, numero("SELECT COUNT(*) FROM movimiento_inventario"));
        assertEquals(0, numero("SELECT COUNT(*) FROM movimiento_puntos"));
        assertTrue(c.getAutoCommit());
    }

    @Test
    void falloSqlTrasDescontarElPrimerComponenteRevierteTodoElCanje() throws Exception {
        darPuntos(60);
        // El producto ID 1 se descuenta antes de intentar registrar su movimiento.
        ejecutar("ALTER TABLE movimiento_inventario ADD CONSTRAINT fallo_canje CHECK (id_producto <> 1)");

        assertThrows(SQLException.class, () -> servicioPuntos().canjear(1, 4));

        assertEquals(60, numero("SELECT saldo_puntos FROM cliente WHERE id_cliente = 1"));
        assertEquals(25, numero("SELECT existencia FROM producto WHERE codigo = 'SAN001'"));
        assertEquals(50, numero("SELECT existencia FROM producto WHERE codigo = 'BEB001'"));
        assertEquals(35, numero("SELECT existencia FROM producto WHERE codigo = 'ACO001'"));
        assertEquals(0, numero("SELECT COUNT(*) FROM canje"));
        assertEquals(0, numero("SELECT COUNT(*) FROM movimiento_inventario"));
        assertEquals(0, numero("SELECT COUNT(*) FROM movimiento_puntos"));
        assertTrue(c.getAutoCommit());
    }

    @Test
    void dosCanjesSimultaneosConPuntosJustosParaUnoSoloNoDejanSaldoNegativo() throws Exception {
        darPuntos(10);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch listas = new CountDownLatch(2), inicio = new CountDownLatch(1);
        try {
            var tareas = new java.util.ArrayList<Future<Boolean>>();
            for (int i = 0; i < 2; i++) {
                tareas.add(pool.submit(() -> {
                    try (Connection otra = DriverManager.getConnection(url)) {
                        listas.countDown();
                        if (!inicio.await(5, TimeUnit.SECONDS)) throw new AssertionError("Timeout");
                        try {
                            new PuntosYCanjesServicio(otra).canjear(1, 1);
                            return true;
                        } catch (PuntosInsuficientesException | StockInsuficienteException e) {
                            return false;
                        }
                    }
                }));
            }
            assertTrue(listas.await(5, TimeUnit.SECONDS));
            inicio.countDown();
            boolean r1 = tareas.get(0).get(10, TimeUnit.SECONDS);
            boolean r2 = tareas.get(1).get(10, TimeUnit.SECONDS);

            assertEquals(1L, (r1 ? 1 : 0) + (r2 ? 1 : 0));
            assertEquals(0, numero("SELECT saldo_puntos FROM cliente WHERE id_cliente = 1"));
            assertEquals(49, numero("SELECT existencia FROM producto WHERE codigo = 'BEB001'"));
            assertEquals(1, numero("SELECT COUNT(*) FROM canje"));
            assertEquals(1, numero("SELECT COUNT(*) FROM movimiento_puntos WHERE tipo = 'CANJE'"));
        } finally {
            inicio.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }
}

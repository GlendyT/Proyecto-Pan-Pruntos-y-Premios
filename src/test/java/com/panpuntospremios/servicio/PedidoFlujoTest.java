package com.panpuntospremios.servicio;

import com.panpuntospremios.dao.*;
import com.panpuntospremios.modelo.*;
import com.panpuntospremios.excepcion.*;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PedidoFlujoTest extends BasePedidoTest {
    @Test void conservaComponentesDelMenuMientrasExistanPedidosPendientes() throws Exception {
        int id = crearMixto();
        Menu menu = new MenuDAOImpl(c).buscarPorCodigo("MEN001");
        menu.getComponentes().removeIf(p -> "SANDWICH".equals(p.getCategoria()));
        menu.getComponentes().add(new ProductoDAOImpl(c).buscarPorCodigo("SAN002"));
        MenuServicio menus = new MenuServicio(c, new MenuDAOImpl(c));
        assertThrows(ValidacionException.class, () -> menus.actualizar(menu));
        assertTrue(new MenuDAOImpl(c).buscarPorCodigo("MEN001").getComponentes().stream()
            .anyMatch(p -> "SAN001".equals(p.getCodigo())));
        servicio(c).anular(id);
        menus.actualizar(menu);
        assertTrue(new MenuDAOImpl(c).buscarPorCodigo("MEN001").getComponentes().stream()
            .anyMatch(p -> "SAN002".equals(p.getCodigo())));
    }

    @Test void rechazaPrecioModificadoTrasCargarCatalogo() throws Exception {
        List<ItemVenta> items = mixto();
        ejecutar("UPDATE producto SET precio = 30 WHERE id_producto = 1");
        assertThrows(ValidacionException.class, () -> servicio(c).crearPedido(cabecera(), items));
        assertEquals(0, numero("SELECT COUNT(*) FROM pedido"));
    }

    @Test void rechazaMenuDesactivadoTrasCargarCatalogo() throws Exception {
        List<ItemVenta> items = mixto();
        ejecutar("UPDATE menu SET activo = 'N' WHERE id_menu = 1");
        assertThrows(ValidacionException.class, () -> servicio(c).crearPedido(cabecera(), items));
        assertEquals(0, numero("SELECT COUNT(*) FROM pedido"));
    }
    @Test void persisteMixtoYRecalculaCabeceraSinConsumirStockNiPuntos() throws Exception {
        int id = crearMixto();
        c.close();
        c = DriverManager.getConnection(url);
        Pedido p = servicio(c).buscarPorId(id);
        assertEquals("PENDIENTE", p.getEstado());
        assertEquals(new BigDecimal("161.00"), p.getTotal());
        assertEquals(28, p.getPuntosGenerados());
        assertNull(p.getMetodoPago());
        var detalle = servicio(c).listarDetalle(id);
        assertEquals(2, detalle.size());
        assertTrue(detalle.stream().anyMatch(d -> "MENU".equals(d.getTipoItem()) && d.getCantidad() == 3));
        assertEquals(25, numero("SELECT existencia FROM producto WHERE id_producto = 1"));
        assertEquals(0, numero("SELECT saldo_puntos FROM cliente WHERE id_cliente = 1"));
        assertEquals(0, numero("SELECT COUNT(*) FROM movimiento_inventario"));
        assertEquals(0, numero("SELECT COUNT(*) FROM movimiento_puntos"));
    }
    @Test void consolidaMenuYProductoParaValidarStock() throws Exception {
        ejecutar("UPDATE producto SET existencia = 4 WHERE id_producto = 1");
        assertThrows(StockInsuficienteException.class, this::crearMixto);
        assertEquals(0, numero("SELECT COUNT(*) FROM pedido"));
    }
    @Test void consultaComponentesActualesDelMenu() throws Exception {
        List<ItemVenta> items = mixto();
        ejecutar("UPDATE menu_detalle SET id_producto = 2 WHERE id_menu = 1 AND rol = 'SANDWICH'");
        ejecutar("UPDATE producto SET existencia = 0 WHERE id_producto = 2");
        assertThrows(StockInsuficienteException.class, () -> servicio(c).crearPedido(cabecera(), items));
    }
    @Test void rechazaClienteYProductoDesactivados() throws Exception {
        List<ItemVenta> items = mixto();
        ejecutar("UPDATE cliente SET activo = 'N' WHERE id_cliente = 1");
        assertThrows(ValidacionException.class, () -> servicio(c).crearPedido(cabecera(), items));
        ejecutar("UPDATE cliente SET activo = 'S' WHERE id_cliente = 1");
        ejecutar("UPDATE producto SET activo = 'N' WHERE id_producto = 1");
        assertThrows(ValidacionException.class, () -> servicio(c).crearPedido(cabecera(), items));
    }
    @Test void revierteCabeceraYDetalleAnteErrorSql() throws Exception {
        ejecutar("ALTER TABLE pedido_detalle ADD CONSTRAINT fallo_detalle CHECK (tipo_item = 'PRODUCTO')");
        assertThrows(SQLException.class, this::crearMixto);
        assertEquals(0, numero("SELECT COUNT(*) FROM pedido"));
        assertEquals(0, numero("SELECT COUNT(*) FROM pedido_detalle"));
        assertTrue(c.getAutoCommit());
    }
    @Test void revierteTambienErroresDeEjecucionDespuesDeInsertar() throws Exception {
        PedidoDAOImpl dao = new PedidoDAOImpl(c) {
            int insertados;
            @Override public void guardarDetalle(PedidoDetalle d) throws SQLException {
                super.guardarDetalle(d);
                if (++insertados == 2) throw new IllegalStateException("Fallo simulado");
            }
        };
        assertThrows(IllegalStateException.class, () -> new PedidoServicio(dao, new ProductoDAOImpl(c), c).crearPedido(cabecera(), mixto()));
        assertEquals(0, numero("SELECT COUNT(*) FROM pedido"));
        assertEquals(0, numero("SELECT COUNT(*) FROM pedido_detalle"));
        assertTrue(c.getAutoCommit());
    }
    @Test void noConfirmaTransaccionAjena() throws Exception {
        c.setAutoCommit(false);
        ejecutar("UPDATE cliente SET nombre = 'Temporal' WHERE id_cliente = 1");
        assertThrows(ValidacionException.class, this::crearMixto);
        assertFalse(c.getAutoCommit()); c.rollback(); c.setAutoCommit(true);
    }
    @Test void noAnulaPedidoPagado() throws Exception {
        int id = crearMixto(); ejecutar("UPDATE pedido SET estado = 'PAGADO' WHERE id_pedido = " + id);
        assertThrows(IllegalArgumentException.class, () -> servicio(c).anular(id));
        assertEquals("PAGADO", servicio(c).buscarPorId(id).getEstado());
    }
}

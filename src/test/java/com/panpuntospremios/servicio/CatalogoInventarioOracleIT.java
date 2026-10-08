package com.panpuntospremios.servicio;

import com.panpuntospremios.dao.MenuDAOImpl;
import com.panpuntospremios.dao.MovimientoInventarioDAOImpl;
import com.panpuntospremios.dao.ProductoDAO;
import com.panpuntospremios.dao.ProductoDAOImpl;
import com.panpuntospremios.modelo.Menu;
import com.panpuntospremios.modelo.Producto;
import com.panpuntospremios.util.ConexionOracle;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CatalogoInventarioOracleIT {

    @Test
    void persisteCatalogoMenuYAbastecimientoEnOracle() throws Exception {
        assumeTrue(Boolean.getBoolean("oracle.it"), "Integración Oracle desactivada por defecto.");

        String sufijo = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        String codigoSandwich = "S" + sufijo;
        String codigoBebida = "B" + sufijo;
        String codigoAcompanamiento = "A" + sufijo;
        String codigoMenu = "M" + sufijo;

        try {
            registrarCatalogo(codigoSandwich, codigoBebida, codigoAcompanamiento, codigoMenu);

            try (Connection conexionReabierta = ConexionOracle.obtener()) {
                ProductoDAO productoDAO = new ProductoDAOImpl(conexionReabierta);
                Producto sandwich = productoDAO.buscarPorCodigo(codigoSandwich);
                Menu menu = new MenuDAOImpl(conexionReabierta).buscarPorCodigo(codigoMenu);

                assertNotNull(sandwich);
                assertEquals(7, sandwich.getExistencia());
                assertNotNull(menu);
                assertEquals(3, menu.getComponentes().size());
            }
        } finally {
            eliminarDatosTemporales(
                codigoMenu,
                List.of(codigoSandwich, codigoBebida, codigoAcompanamiento)
            );
        }
    }

    private void registrarCatalogo(
        String codigoSandwich,
        String codigoBebida,
        String codigoAcompanamiento,
        String codigoMenu
    ) throws Exception {
        try (Connection conexion = ConexionOracle.obtener()) {
            ProductoDAO productoDAO = new ProductoDAOImpl(conexion);
            ProductoServicio productoServicio = new ProductoServicio(productoDAO);
            productoServicio.registrar(producto(codigoSandwich, "SANDWICH", "Sándwich temporal", "25.00"));
            productoServicio.registrar(producto(codigoBebida, "BEBIDA", "Bebida temporal", "8.00"));
            productoServicio.registrar(producto(codigoAcompanamiento, "ACOMPANAMIENTO", "Acompañamiento temporal", "10.00"));

            Producto sandwich = productoDAO.buscarPorCodigo(codigoSandwich);
            Producto bebida = productoDAO.buscarPorCodigo(codigoBebida);
            Producto acompanamiento = productoDAO.buscarPorCodigo(codigoAcompanamiento);

            Menu menu = new Menu(0, codigoMenu, "Menú temporal", new BigDecimal("37.00"), true);
            menu.setComponentes(List.of(sandwich, bebida, acompanamiento));
            new MenuServicio(conexion, new MenuDAOImpl(conexion)).registrar(menu);

            InventarioServicio inventario = new InventarioServicio(
                conexion,
                productoDAO,
                new MovimientoInventarioDAOImpl(conexion)
            );
            inventario.abastecer(sandwich.getIdProducto(), 7);
        }
    }

    private Producto producto(String codigo, String categoria, String nombre, String precio) {
        return new Producto(0, codigo, categoria, nombre, new BigDecimal(precio), 0, true);
    }

    private void eliminarDatosTemporales(String codigoMenu, List<String> codigosProductos) throws Exception {
        try (Connection conexion = ConexionOracle.obtener()) {
            ejecutar(conexion, """
                DELETE FROM menu_detalle
                 WHERE id_menu IN (SELECT id_menu FROM menu WHERE codigo = ?)
                """, List.of(codigoMenu));
            ejecutar(conexion, "DELETE FROM menu WHERE codigo = ?", List.of(codigoMenu));
            ejecutar(conexion, """
                DELETE FROM movimiento_inventario
                 WHERE id_producto IN (
                    SELECT id_producto FROM producto WHERE codigo IN (?, ?, ?)
                 )
                """, codigosProductos);
            ejecutar(
                conexion,
                "DELETE FROM producto WHERE codigo IN (?, ?, ?)",
                codigosProductos
            );
        }
    }

    private void ejecutar(Connection conexion, String sql, List<String> parametros) throws Exception {
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setString(i + 1, parametros.get(i));
            }
            ps.executeUpdate();
        }
    }
}

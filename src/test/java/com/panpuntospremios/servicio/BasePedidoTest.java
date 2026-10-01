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

abstract class BasePedidoTest {
    Connection c;
    String url;
    @BeforeEach void preparar() throws Exception {
        url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=Oracle;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000";
        c = DriverManager.getConnection(url);
        // Ejecuta el esquema real y sus datos de ejemplo exclusivamente en H2.
        try (Statement st = c.createStatement()) {
            for (String sql : Files.readString(Path.of("sql/ddl.sql")).split(";"))
                if (!sql.isBlank()) st.execute(sql);
        }
    }
    @AfterEach void cerrar() throws Exception {
        try (Connection limpieza = DriverManager.getConnection(url); Statement st = limpieza.createStatement()) {
            st.execute("DROP ALL OBJECTS");
        }
        c.close();
    }
    PedidoServicio servicio(Connection conexion) {
        return new PedidoServicio(new PedidoDAOImpl(conexion), new ProductoDAOImpl(conexion), conexion);
    }
    List<ItemVenta> mixto() throws Exception {
        return List.of(new ProductoIndividual(new ProductoDAOImpl(c).buscarPorCodigo("SAN001"), 2),
            new MenuCompleto(new MenuDAOImpl(c).buscarPorCodigo("MEN001"), 3));
    }
    Pedido cabecera() { return new Pedido(0, 1, null, "PAGADO", BigDecimal.ONE, 999, "TARJETA", "ignorar"); }
    int crearMixto() throws Exception { return servicio(c).crearPedido(cabecera(), mixto()); }
    int numero(String sql) throws Exception {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) { rs.next(); return rs.getInt(1); }
    }
    void ejecutar(String sql) throws Exception { try (Statement st = c.createStatement()) { st.execute(sql); } }

}

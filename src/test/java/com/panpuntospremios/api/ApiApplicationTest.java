package com.panpuntospremios.api;

import com.panpuntospremios.api.controller.HealthController;
import com.panpuntospremios.api.dto.ErrorRespuestaDTO;
import com.panpuntospremios.api.excepcion.GlobalExceptionHandler;
import com.panpuntospremios.excepcion.PagoRechazadoException;
import com.panpuntospremios.excepcion.PuntosInsuficientesException;
import com.panpuntospremios.excepcion.StockInsuficienteException;
import com.panpuntospremios.excepcion.ValidacionException;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ApiApplication.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:test_api;MODE=Oracle;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver"
})
class ApiApplicationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private HealthController healthController;

    @Test
    void testContextoLoadsYConfiguracionHikariLimitada() {
        assertNotNull(dataSource, "El DataSource debe estar configurado por Spring Boot");
        assertInstanceOf(HikariDataSource.class, dataSource, "El pool debe ser HikariCP");

        HikariDataSource hikari = (HikariDataSource) dataSource;
        assertEquals("PanPuntosHikariPool", hikari.getPoolName());
        assertEquals(5, hikari.getMaximumPoolSize(), "El tamaño máximo del pool debe estar acotado a 5");
        assertTrue(hikari.getConnectionTimeout() > 0);
    }

    @Test
    void testHealthEndpointRetornaEstadoCorrecto() {
        ResponseEntity<Map<String, Object>> respuesta = healthController.health();
        assertEquals(HttpStatus.OK, respuesta.getStatusCode());

        Map<String, Object> cuerpo = respuesta.getBody();
        assertNotNull(cuerpo);
        assertEquals("UP", cuerpo.get("status"));
        assertEquals("UP", cuerpo.get("baseDatos"));
        assertEquals("pan-puntos-premios-api", cuerpo.get("servicio"));
        assertNotNull(cuerpo.get("timestamp"));
    }

    @Test
    void testGlobalExceptionHandlerCapturaExcepcionesDelNegocio() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        // 1. ValidacionException -> 400
        ResponseEntity<ErrorRespuestaDTO> rVal = handler.manejarValidacion(new ValidacionException("Nombre obligatorio"));
        assertEquals(HttpStatus.BAD_REQUEST, rVal.getStatusCode());
        assertEquals("Nombre obligatorio", rVal.getBody().getMensaje());
        assertEquals(400, rVal.getBody().getStatus());

        // 2. StockInsuficienteException -> 400
        ResponseEntity<ErrorRespuestaDTO> rStock = handler.manejarStockInsuficiente(new StockInsuficienteException("Sin stock disponible"));
        assertEquals(HttpStatus.BAD_REQUEST, rStock.getStatusCode());
        assertEquals("Sin stock disponible", rStock.getBody().getMensaje());

        // 3. PuntosInsuficientesException -> 400
        ResponseEntity<ErrorRespuestaDTO> rPuntos = handler.manejarPuntosInsuficientes(new PuntosInsuficientesException("Saldo insuficiente"));
        assertEquals(HttpStatus.BAD_REQUEST, rPuntos.getStatusCode());
        assertEquals("Saldo insuficiente", rPuntos.getBody().getMensaje());

        // 4. PagoRechazadoException -> 400
        ResponseEntity<ErrorRespuestaDTO> rPago = handler.manejarPagoRechazado(new PagoRechazadoException("Tarjeta denegada"));
        assertEquals(HttpStatus.BAD_REQUEST, rPago.getStatusCode());
        assertEquals("Tarjeta denegada", rPago.getBody().getMensaje());

        // 5. SQLException -> 500 sin exponer detalles SQL
        ResponseEntity<ErrorRespuestaDTO> rSql = handler.manejarSqlException(new SQLException("ORA-00001: unique constraint violated"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, rSql.getStatusCode());
        assertEquals("Ocurrió un error interno al consultar la base de datos", rSql.getBody().getMensaje());
        assertFalse(rSql.getBody().getMensaje().contains("ORA-00001"), "No debe filtrar mensajes internos de BD");

        // 6. Exception general -> 500
        ResponseEntity<ErrorRespuestaDTO> rGen = handler.manejarExcepcionGeneral(new RuntimeException("Error inesperado"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, rGen.getStatusCode());
        assertEquals("Ocurrió un error inesperado en el servidor", rGen.getBody().getMensaje());
    }
}

package com.panpuntospremios.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("timestamp", Instant.now().toString());
        respuesta.put("servicio", "pan-puntos-premios-api");

        boolean bdConectada = false;
        try (Connection con = dataSource.getConnection()) {
            bdConectada = con != null && con.isValid(2);
        } catch (Exception e) {
            bdConectada = false;
        }

        if (bdConectada) {
            respuesta.put("status", "UP");
            respuesta.put("baseDatos", "UP");
            return ResponseEntity.ok(respuesta);
        } else {
            respuesta.put("status", "DOWN");
            respuesta.put("baseDatos", "DOWN");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(respuesta);
        }
    }
}

package com.panpuntospremios.api.controller;

import com.panpuntospremios.api.dto.AuthMensajeDTO;
import com.panpuntospremios.api.dto.ConsumirTokenDTO;
import com.panpuntospremios.api.dto.SolicitudAccesoDTO;
import com.panpuntospremios.api.servicio.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/access-requests")
    public ResponseEntity<AuthMensajeDTO> solicitarAcceso(
            @Valid @RequestBody SolicitudAccesoDTO dto,
            HttpServletRequest request) {

        String ip = obtenerDireccionIp(request);
        String userAgent = request.getHeader("User-Agent");

        AuthMensajeDTO respuesta = authService.solicitarAcceso(dto.getEmail(), ip, userAgent);
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/consume")
    public ResponseEntity<AuthMensajeDTO> consumirToken(
            @Valid @RequestBody ConsumirTokenDTO dto,
            HttpServletRequest request,
            HttpServletResponse response) {

        AuthMensajeDTO respuesta = authService.consumirToken(dto.getToken(), request, response);
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthMensajeDTO> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        AuthMensajeDTO respuesta = authService.cerrarSesion(request, response);
        return ResponseEntity.ok(respuesta);
    }

    private String obtenerDireccionIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

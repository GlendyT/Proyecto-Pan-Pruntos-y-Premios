package com.panpuntospremios.api.controller;

import com.panpuntospremios.api.dto.MiPerfilDTO;
import com.panpuntospremios.api.dto.PaginaMovimientosDTO;
import com.panpuntospremios.api.dto.SaldoPuntosDTO;
import com.panpuntospremios.api.modelo.ClientePrincipal;
import com.panpuntospremios.api.servicio.PuntosConsultaService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;

@RestController
@RequestMapping("/api/v1/me")
public class PuntosController {

    private final PuntosConsultaService puntosService;

    public PuntosController(PuntosConsultaService puntosService) {
        this.puntosService = puntosService;
    }

    @GetMapping
    public ResponseEntity<MiPerfilDTO> miPerfil(Authentication authentication) throws SQLException {
        return privada(puntosService.obtenerPerfil(clienteAutenticado(authentication)));
    }

    @GetMapping("/points")
    public ResponseEntity<SaldoPuntosDTO> miSaldo(Authentication authentication) throws SQLException {
        return privada(puntosService.obtenerSaldo(clienteAutenticado(authentication)));
    }

    @GetMapping("/points/movements")
    public ResponseEntity<PaginaMovimientosDTO> misMovimientos(
            Authentication authentication,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "before", required = false) String before) throws SQLException {
        return privada(puntosService.listarMovimientos(
            clienteAutenticado(authentication), limit, before));
    }

    private ClientePrincipal clienteAutenticado(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof ClientePrincipal principal)) {
            throw new BadCredentialsException("La sesión de cliente no es válida.");
        }
        return principal;
    }

    private <T> ResponseEntity<T> privada(T body) {
        return ResponseEntity.ok()
            .cacheControl(CacheControl.noStore().mustRevalidate())
            .body(body);
    }
}

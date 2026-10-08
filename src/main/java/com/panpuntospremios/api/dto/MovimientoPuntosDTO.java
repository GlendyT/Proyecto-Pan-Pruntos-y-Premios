package com.panpuntospremios.api.dto;

import java.time.Instant;

public record MovimientoPuntosDTO(
    int idMovimiento,
    Instant fecha,
    String tipo,
    int puntos,
    String referencia
) {
}

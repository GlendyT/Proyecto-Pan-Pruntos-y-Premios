package com.panpuntospremios.api.dto;

import java.util.List;

public record PaginaMovimientosDTO(
    List<MovimientoPuntosDTO> movimientos,
    String siguienteCursor,
    boolean hayMas
) {
}

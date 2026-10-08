package com.panpuntospremios.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Resultado tabular de un reporte, con encabezados y filas en el mismo orden. */
public record ReporteDatos(String titulo, List<String> columnas, List<List<Object>> filas) {
    public ReporteDatos {
        columnas = List.copyOf(columnas);
        filas = filas.stream()
            .map(fila -> Collections.unmodifiableList(new ArrayList<>(fila)))
            .toList();
    }
}

package com.panpuntospremios.servicio;

import com.panpuntospremios.dao.ReporteDAO;
import com.panpuntospremios.excepcion.ValidacionException;
import com.panpuntospremios.modelo.ReporteDatos;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Set;

public class ReporteServicio {
    public enum Tipo { VENTAS, INVENTARIO, MOVIMIENTOS_INVENTARIO, MOVIMIENTOS_PUNTOS }
    private final ReporteDAO dao;
    public ReporteServicio(ReporteDAO dao) { this.dao = dao; }

    public ReporteDatos consultar(Tipo tipo, LocalDate desde, LocalDate hasta, String filtro) throws SQLException {
        if (tipo == null) throw new ValidacionException("Selecciona un reporte.");
        if (desde != null && hasta != null && desde.isAfter(hasta))
            throw new ValidacionException("La fecha inicial no puede ser posterior a la fecha final.");
        String valor = filtro == null ? "TODOS" : filtro;
        return switch (tipo) {
            case VENTAS -> {
                validarFiltro(valor, Set.of("TODOS", "PAGADO", "ENTREGADO"));
                yield dao.consultarVentas(desde, hasta, valor);
            }
            case INVENTARIO -> {
                validarFiltro(valor, Set.of("TODOS", "ACTIVOS", "INACTIVOS"));
                yield dao.consultarInventario(switch (valor) {
                    case "ACTIVOS" -> "S";
                    case "INACTIVOS" -> "N";
                    default -> "TODOS";
                });
            }
            case MOVIMIENTOS_INVENTARIO -> {
                validarFiltro(valor, Set.of("TODOS", "ABASTECIMIENTO", "VENTA", "CANJE"));
                yield dao.consultarMovimientosInventario(desde, hasta, valor);
            }
            case MOVIMIENTOS_PUNTOS -> {
                validarFiltro(valor, Set.of("TODOS", "ACUMULACION", "CANJE"));
                yield dao.consultarMovimientosPuntos(desde, hasta, valor);
            }
        };
    }

    private static void validarFiltro(String filtro, Set<String> permitidos) {
        if (!permitidos.contains(filtro)) throw new ValidacionException("El filtro seleccionado no es válido para este reporte.");
    }
}

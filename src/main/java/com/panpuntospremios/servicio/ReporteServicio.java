package com.panpuntospremios.servicio;

import com.panpuntospremios.dao.ReporteDAO;
import com.panpuntospremios.excepcion.ValidacionException;
import com.panpuntospremios.modelo.ReporteDatos;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Set;

public class ReporteServicio {
    public enum Tipo {
        VENTAS,
        INVENTARIO,
        MOVIMIENTOS_INVENTARIO,
        MOVIMIENTOS_PUNTOS,
        // Reportes del enunciado
        VENTAS_POR_PERIODO,
        PRODUCTOS_MAS_VENDIDOS,
        BAJO_INVENTARIO,
        PUNTOS_POR_CLIENTE,
        HISTORIAL_CANJES,
        VENTAS_MENUS
    }

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
            case VENTAS_POR_PERIODO -> {
                validarFiltro(valor, Set.of("TODOS", "EFECTIVO", "TARJETA"));
                yield dao.consultarVentasPorPeriodo(desde, hasta, valor);
            }
            case PRODUCTOS_MAS_VENDIDOS -> dao.consultarProductosMasVendidos();
            case BAJO_INVENTARIO -> dao.consultarBajoInventario();
            case PUNTOS_POR_CLIENTE -> dao.consultarPuntosPorCliente();
            case HISTORIAL_CANJES -> dao.consultarHistorialCanjes();
            case VENTAS_MENUS -> dao.consultarVentasMenus();
        };
    }

    public ReporteDatos consultarVentasPorPeriodo(LocalDate desde, LocalDate hasta, String metodo) throws SQLException {
        return consultar(Tipo.VENTAS_POR_PERIODO, desde, hasta, metodo);
    }

    public ReporteDatos consultarProductosMasVendidos() throws SQLException {
        return consultar(Tipo.PRODUCTOS_MAS_VENDIDOS, null, null, "TODOS");
    }

    public ReporteDatos consultarBajoInventario() throws SQLException {
        return consultar(Tipo.BAJO_INVENTARIO, null, null, "TODOS");
    }

    public ReporteDatos consultarPuntosPorCliente() throws SQLException {
        return consultar(Tipo.PUNTOS_POR_CLIENTE, null, null, "TODOS");
    }

    public ReporteDatos consultarHistorialCanjes() throws SQLException {
        return consultar(Tipo.HISTORIAL_CANJES, null, null, "TODOS");
    }

    public ReporteDatos consultarVentasMenus() throws SQLException {
        return consultar(Tipo.VENTAS_MENUS, null, null, "TODOS");
    }

    private static void validarFiltro(String filtro, Set<String> permitidos) {
        if (!permitidos.contains(filtro)) throw new ValidacionException("El filtro seleccionado no es válido para este reporte.");
    }
}

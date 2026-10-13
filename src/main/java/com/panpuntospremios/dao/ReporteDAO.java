package com.panpuntospremios.dao;

import com.panpuntospremios.modelo.ReporteDatos;
import java.sql.SQLException;
import java.time.LocalDate;

public interface ReporteDAO {
    // Métodos generales existentes
    ReporteDatos consultarVentas(LocalDate desde, LocalDate hasta, String estado) throws SQLException;
    ReporteDatos consultarInventario(String estado) throws SQLException;
    ReporteDatos consultarMovimientosInventario(LocalDate desde, LocalDate hasta, String tipo) throws SQLException;
    ReporteDatos consultarMovimientosPuntos(LocalDate desde, LocalDate hasta, String tipo) throws SQLException;

    // Métodos obligatorios del enunciado (Sección 14)
    ReporteDatos consultarVentasPorPeriodo(LocalDate desde, LocalDate hasta, String metodoPago) throws SQLException;
    ReporteDatos consultarProductosMasVendidos() throws SQLException;
    ReporteDatos consultarBajoInventario() throws SQLException;
    ReporteDatos consultarPuntosPorCliente() throws SQLException;
    ReporteDatos consultarHistorialCanjes() throws SQLException;
    ReporteDatos consultarVentasMenus() throws SQLException;
}

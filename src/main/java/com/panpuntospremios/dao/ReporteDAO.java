package com.panpuntospremios.dao;

import com.panpuntospremios.modelo.ReporteDatos;
import java.sql.SQLException;
import java.time.LocalDate;

public interface ReporteDAO {
    ReporteDatos consultarVentas(LocalDate desde, LocalDate hasta, String estado) throws SQLException;
    ReporteDatos consultarInventario(String estado) throws SQLException;
    ReporteDatos consultarMovimientosInventario(LocalDate desde, LocalDate hasta, String tipo) throws SQLException;
    ReporteDatos consultarMovimientosPuntos(LocalDate desde, LocalDate hasta, String tipo) throws SQLException;
}

package com.panpuntospremios.api.dao;

import com.panpuntospremios.modelo.MovimientoPuntos;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface PuntosConsultaDAO {

    Optional<ClienteResumen> buscarClienteActivo(int idCliente) throws SQLException;

    List<MovimientoPuntos> listarMovimientos(
        int idCliente,
        Timestamp antesDe,
        Integer antesDeId,
        int limite
    ) throws SQLException;

    record ClienteResumen(int idCliente, String nombre, int saldoPuntos) {
    }
}

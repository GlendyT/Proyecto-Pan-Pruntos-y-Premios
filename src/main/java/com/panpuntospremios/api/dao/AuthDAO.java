package com.panpuntospremios.api.dao;

import com.panpuntospremios.api.modelo.ClienteAuth;

import java.sql.Timestamp;
import java.util.Optional;

public interface AuthDAO {

    Optional<ClienteAuth> buscarClientePorEmail(String email);

    Optional<ClienteAuth> buscarClientePorId(int idCliente);

    boolean superaLimiteSolicitudes(int idCliente, int cooldownSegundos, int ventanaMinutos, int maxSolicitudes);

    void registrarToken(int idCliente, String tokenHash, Timestamp expiracion, String ip, String userAgent);

    Optional<Integer> consumirToken(String tokenHash);
}

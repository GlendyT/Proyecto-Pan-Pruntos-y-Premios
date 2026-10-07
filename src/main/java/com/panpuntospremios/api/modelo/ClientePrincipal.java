package com.panpuntospremios.api.modelo;

import org.springframework.security.core.AuthenticatedPrincipal;

import java.io.Serializable;

public class ClientePrincipal implements AuthenticatedPrincipal, Serializable {

    private static final long serialVersionUID = 1L;

    private final int idCliente;
    private final String identificador;
    private final String nombre;
    private final String email;

    public ClientePrincipal(int idCliente, String identificador, String nombre, String email) {
        this.idCliente = idCliente;
        this.identificador = identificador;
        this.nombre = nombre;
        this.email = email;
    }

    @Override
    public String getName() {
        return String.valueOf(idCliente);
    }

    public int getIdCliente() {
        return idCliente;
    }

    public String getIdentificador() {
        return identificador;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }
}

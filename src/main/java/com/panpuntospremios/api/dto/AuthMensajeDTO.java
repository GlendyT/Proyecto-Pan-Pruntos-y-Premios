package com.panpuntospremios.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthMensajeDTO {

    private String mensaje;
    private Integer idCliente;
    private String identificador;
    private String nombre;
    private String email;

    public AuthMensajeDTO() {}

    public AuthMensajeDTO(String mensaje) {
        this.mensaje = mensaje;
    }

    public AuthMensajeDTO(String mensaje, Integer idCliente, String identificador, String nombre, String email) {
        this.mensaje = mensaje;
        this.idCliente = idCliente;
        this.identificador = identificador;
        this.nombre = nombre;
        this.email = email;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public Integer getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Integer idCliente) {
        this.idCliente = idCliente;
    }

    public String getIdentificador() {
        return identificador;
    }

    public void setIdentificador(String identificador) {
        this.identificador = identificador;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}

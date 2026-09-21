package com.panpuntospremios.modelo;

import java.util.Date;

public class Cliente {
    private int idCliente;
    private String identificador;
    private String nombre;
    private String telefono;
    private String email;
    private int saldoPuntos;
    private boolean activo;
    private Date fechaRegistro;

    public Cliente(int idCliente, String identificador, String nombre, String telefono, String email, int saldoPuntos, boolean activo, Date fechaRegistro) {
        this.idCliente = idCliente;
        this.identificador = identificador;
        this.nombre = nombre;
        this.telefono = telefono;
        this.email = email;
        this.saldoPuntos = saldoPuntos;
        this.activo = activo;
        this.fechaRegistro = fechaRegistro;
    }

    public int getIdCliente() { return idCliente; }
    public String getIdentificador() { return identificador; }
    public String getNombre() { return nombre; }
    public String getTelefono() { return telefono; }
    public String getEmail() { return email; }
    public int getSaldoPuntos() { return saldoPuntos; }
    public boolean isActivo() { return activo; }
    public Date getFechaRegistro() { return fechaRegistro; }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public void setEmail(String email) { this.email = email; }
    public void setSaldoPuntos(int saldoPuntos) { this.saldoPuntos = saldoPuntos; }
    public void setActivo(boolean activo) { this.activo = activo; }
}

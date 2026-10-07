package com.panpuntospremios.api.modelo;

public class ClienteAuth {
    private int idCliente;
    private String identificador;
    private String nombre;
    private String telefono;
    private String email;
    private int saldoPuntos;
    private boolean activo;
    private boolean emailVerificado;

    public ClienteAuth(int idCliente, String identificador, String nombre, String telefono, String email, int saldoPuntos, boolean activo, boolean emailVerificado) {
        this.idCliente = idCliente;
        this.identificador = identificador;
        this.nombre = nombre;
        this.telefono = telefono;
        this.email = email;
        this.saldoPuntos = saldoPuntos;
        this.activo = activo;
        this.emailVerificado = emailVerificado;
    }

    public int getIdCliente() { return idCliente; }
    public String getIdentificador() { return identificador; }
    public String getNombre() { return nombre; }
    public String getTelefono() { return telefono; }
    public String getEmail() { return email; }
    public int getSaldoPuntos() { return saldoPuntos; }
    public boolean isActivo() { return activo; }
    public boolean isEmailVerificado() { return emailVerificado; }
}

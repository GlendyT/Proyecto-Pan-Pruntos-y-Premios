package com.panpuntospremios.modelo;

import java.util.Date;

public class Canje {
    private int idCanje;
    private int idCliente;
    private int idRecompensa;
    private Date fecha;
    private int puntosUtilizados;

    public Canje(int idCanje, int idCliente, int idRecompensa, Date fecha, int puntosUtilizados) {
        this.idCanje = idCanje;
        this.idCliente = idCliente;
        this.idRecompensa = idRecompensa;
        this.fecha = fecha;
        this.puntosUtilizados = puntosUtilizados;
    }

    public int getIdCanje() { return idCanje; }
    public int getIdCliente() { return idCliente; }
    public int getIdRecompensa() { return idRecompensa; }
    public Date getFecha() { return fecha; }
    public int getPuntosUtilizados() { return puntosUtilizados; }
}

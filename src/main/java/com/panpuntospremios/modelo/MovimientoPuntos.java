package com.panpuntospremios.modelo;

import java.util.Date;

public class MovimientoPuntos {
    private int idMovimiento;
    private int idCliente;
    private Date fecha;
    private String tipo;
    private int puntos;
    private String referencia;

    public MovimientoPuntos(int idMovimiento, int idCliente, Date fecha, String tipo, int puntos, String referencia) {
        this.idMovimiento = idMovimiento;
        this.idCliente = idCliente;
        this.fecha = fecha;
        this.tipo = tipo;
        this.puntos = puntos;
        this.referencia = referencia;
    }

    public int getIdMovimiento() { return idMovimiento; }
    public int getIdCliente() { return idCliente; }
    public Date getFecha() { return fecha; }
    public String getTipo() { return tipo; }
    public int getPuntos() { return puntos; }
    public String getReferencia() { return referencia; }
}

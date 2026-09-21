package com.panpuntospremios.modelo;

import java.util.Date;

public class MovimientoInventario {
    private int idMovimiento;
    private int idProducto;
    private Date fecha;
    private String tipo;
    private int cantidad;
    private int existenciaResultante;

    public MovimientoInventario(int idMovimiento, int idProducto, Date fecha, String tipo, int cantidad, int existenciaResultante) {
        this.idMovimiento = idMovimiento;
        this.idProducto = idProducto;
        this.fecha = fecha;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.existenciaResultante = existenciaResultante;
    }

    public int getIdMovimiento() { return idMovimiento; }
    public int getIdProducto() { return idProducto; }
    public Date getFecha() { return fecha; }
    public String getTipo() { return tipo; }
    public int getCantidad() { return cantidad; }
    public int getExistenciaResultante() { return existenciaResultante; }
}

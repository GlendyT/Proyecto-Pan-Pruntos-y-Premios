package com.panpuntospremios.modelo;

import java.math.BigDecimal;

public class Producto {
    private int idProducto;
    private String codigo;
    private String categoria;
    private String nombre;
    private BigDecimal precio;
    private int existencia;
    private boolean activo;

    public Producto(int idProducto, String codigo, String categoria, String nombre, BigDecimal precio, int existencia, boolean activo) {
        this.idProducto = idProducto;
        this.codigo = codigo;
        this.categoria = categoria;
        this.nombre = nombre;
        this.precio = precio;
        this.existencia = existencia;
        this.activo = activo;
    }

    public int getIdProducto() { return idProducto; }
    public String getCodigo() { return codigo; }
    public String getCategoria() { return categoria; }
    public String getNombre() { return nombre; }
    public BigDecimal getPrecio() { return precio; }
    public int getExistencia() { return existencia; }
    public boolean isActivo() { return activo; }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public void setExistencia(int existencia) { this.existencia = existencia; }
    public void setActivo(boolean activo) { this.activo = activo; }
}

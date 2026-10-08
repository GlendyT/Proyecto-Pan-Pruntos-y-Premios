package com.panpuntospremios.modelo;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

public class Menu {
    private int idMenu;
    private String codigo;
    private String nombre;
    private BigDecimal precio;
    private boolean activo;
    private List<Producto> componentes;

    public Menu(int idMenu, String codigo, String nombre, BigDecimal precio, boolean activo) {
        this.idMenu = idMenu;
        this.codigo = codigo;
        this.nombre = nombre;
        this.precio = precio;
        this.activo = activo;
        this.componentes = new ArrayList<>();
    }

    public int getIdMenu() { return idMenu; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public BigDecimal getPrecio() { return precio; }
    public boolean isActivo() { return activo; }
    public List<Producto> getComponentes() { return componentes; }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public void setComponentes(List<Producto> componentes) { this.componentes = componentes; }

    @Override
    public String toString() { return nombre; }
}

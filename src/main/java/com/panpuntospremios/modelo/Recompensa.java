package com.panpuntospremios.modelo;

public class Recompensa {
    private int idRecompensa;
    private String nombre;
    private int puntosNecesarios;
    private Integer idProducto;
    private Integer idMenu;
    private boolean activo;

    public Recompensa(int idRecompensa, String nombre, int puntosNecesarios, Integer idProducto, Integer idMenu, boolean activo) {
        this.idRecompensa = idRecompensa;
        this.nombre = nombre;
        this.puntosNecesarios = puntosNecesarios;
        this.idProducto = idProducto;
        this.idMenu = idMenu;
        this.activo = activo;
    }

    public int getIdRecompensa() { return idRecompensa; }
    public String getNombre() { return nombre; }
    public int getPuntosNecesarios() { return puntosNecesarios; }
    public Integer getIdProducto() { return idProducto; }
    public Integer getIdMenu() { return idMenu; }
    public boolean isActivo() { return activo; }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setPuntosNecesarios(int puntosNecesarios) { this.puntosNecesarios = puntosNecesarios; }
    public void setActivo(boolean activo) { this.activo = activo; }
}

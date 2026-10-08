package com.panpuntospremios.modelo;

import java.math.BigDecimal;
import java.util.Date;

public class Pedido {
    private int idPedido;
    private int idCliente;
    private Date fecha;
    private String estado;
    private BigDecimal total;
    private int puntosGenerados;
    private String metodoPago;
    private String referenciaPago;

    public Pedido(int idPedido, int idCliente, Date fecha, String estado, BigDecimal total, int puntosGenerados, String metodoPago, String referenciaPago) {
        this.idPedido = idPedido;
        this.idCliente = idCliente;
        this.fecha = fecha;
        this.estado = estado;
        this.total = total;
        this.puntosGenerados = puntosGenerados;
        this.metodoPago = metodoPago;
        this.referenciaPago = referenciaPago;
    }

    public int getIdPedido() { return idPedido; }
    public int getIdCliente() { return idCliente; }
    public Date getFecha() { return fecha; }
    public String getEstado() { return estado; }
    public BigDecimal getTotal() { return total; }
    public int getPuntosGenerados() { return puntosGenerados; }
    public String getMetodoPago() { return metodoPago; }
    public String getReferenciaPago() { return referenciaPago; }

    public void setEstado(String estado) { this.estado = estado; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public void setPuntosGenerados(int puntosGenerados) { this.puntosGenerados = puntosGenerados; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
    public void setReferenciaPago(String referenciaPago) { this.referenciaPago = referenciaPago; }
}

package com.panpuntospremios.dao;

import com.panpuntospremios.modelo.Pedido;
import com.panpuntospremios.modelo.PedidoDetalle;
import java.sql.SQLException;
import java.util.List;

public interface PedidoDAO {
    int guardar(Pedido pedido) throws SQLException;
    void guardarDetalle(PedidoDetalle detalle) throws SQLException;
    Pedido buscarPorId(int idPedido) throws SQLException;
    List<Pedido> listarPendientesPorCliente(int idCliente) throws SQLException;
    List<Pedido> listarPorCliente(int idCliente) throws SQLException;
    List<PedidoDetalle> listarDetalle(int idPedido) throws SQLException;
    void cambiarEstado(int idPedido, String estado) throws SQLException;
}

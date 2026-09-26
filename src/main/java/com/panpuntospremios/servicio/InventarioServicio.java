package com.panpuntospremios.servicio;

import com.panpuntospremios.dao.MovimientoInventarioDAO;
import com.panpuntospremios.dao.ProductoDAO;
import com.panpuntospremios.excepcion.ValidacionException;
import com.panpuntospremios.modelo.MovimientoInventario;
import com.panpuntospremios.modelo.Producto;
import com.panpuntospremios.util.Transaccion;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class InventarioServicio {
    private final Connection conexion;
    private final ProductoDAO productoDAO;
    private final MovimientoInventarioDAO movimientoDAO;

    public InventarioServicio(
        Connection conexion,
        ProductoDAO productoDAO,
        MovimientoInventarioDAO movimientoDAO
    ) {
        this.conexion = conexion;
        this.productoDAO = productoDAO;
        this.movimientoDAO = movimientoDAO;
    }

    public void abastecer(int idProducto, int cantidad) throws SQLException {
        if (idProducto <= 0) {
            throw new ValidacionException("Selecciona un producto válido.");
        }
        if (cantidad <= 0) {
            throw new ValidacionException("La cantidad de abastecimiento debe ser mayor que cero.");
        }

        Transaccion.ejecutar(conexion, () -> {
            Producto producto = productoDAO.bloquearPorId(idProducto);
            if (producto == null || !producto.isActivo()) {
                throw new ValidacionException("El producto no existe o está inactivo.");
            }
            int nuevaExistencia;
            try {
                nuevaExistencia = Math.addExact(producto.getExistencia(), cantidad);
            } catch (ArithmeticException e) {
                throw new ValidacionException("La existencia resultante supera el límite permitido.");
            }
            productoDAO.actualizarExistencia(idProducto, nuevaExistencia);
            movimientoDAO.registrar(new MovimientoInventario(
                0,
                idProducto,
                null,
                "ABASTECIMIENTO",
                cantidad,
                nuevaExistencia
            ));
            return null;
        });
    }

    public List<Producto> listarProductos() throws SQLException {
        return productoDAO.listarActivos();
    }

    public List<MovimientoInventario> listarMovimientos() throws SQLException {
        return movimientoDAO.listarRecientes();
    }
}

package com.panpuntospremios.servicio;

import com.panpuntospremios.dao.ProductoDAO;
import com.panpuntospremios.enums.Categoria;
import com.panpuntospremios.excepcion.ValidacionException;
import com.panpuntospremios.modelo.Producto;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

public class ProductoServicio {
    private static final Pattern PATRON_CODIGO = Pattern.compile("^[A-Z0-9][A-Z0-9_-]{0,19}$");
    private static final Pattern PATRON_NOMBRE = Pattern.compile(
        "^[\\p{L}\\p{N}]+(?:[ .,'()\\-][\\p{L}\\p{N}]+)*$",
        Pattern.UNICODE_CHARACTER_CLASS
    );

    private final ProductoDAO productoDAO;

    public ProductoServicio(ProductoDAO productoDAO) {
        this.productoDAO = productoDAO;
    }

    public void registrar(Producto producto) throws SQLException {
        validar(producto, true);
        if (productoDAO.buscarPorCodigo(producto.getCodigo()) != null) {
            throw new ValidacionException("Ya existe un producto con ese código.");
        }
        productoDAO.registrar(producto);
    }

    public List<Producto> listarActivos() throws SQLException {
        return productoDAO.listarActivos();
    }

    public Producto buscarPorId(int idProducto) throws SQLException {
        return productoDAO.buscarPorId(idProducto);
    }

    public void actualizar(Producto producto) throws SQLException {
        validar(producto, false);
        if (producto.getIdProducto() <= 0) {
            throw new ValidacionException("El ID del producto debe ser mayor que cero.");
        }
        productoDAO.actualizar(producto);
    }

    public void desactivar(int idProducto) throws SQLException {
        if (idProducto <= 0) {
            throw new ValidacionException("El ID del producto debe ser mayor que cero.");
        }
        productoDAO.desactivar(idProducto);
    }

    private void validar(Producto producto, boolean validarCodigoYCategoria) {
        if (producto == null) {
            throw new ValidacionException("Los datos del producto son obligatorios.");
        }
        if (validarCodigoYCategoria) {
            if (producto.getCodigo() == null || !PATRON_CODIGO.matcher(producto.getCodigo()).matches()) {
                throw new ValidacionException(
                    "El código debe tener de 1 a 20 caracteres en mayúsculas, números, _ o -."
                );
            }
            try {
                Categoria.valueOf(producto.getCategoria());
            } catch (Exception e) {
                throw new ValidacionException("La categoría del producto no es válida.");
            }
        }
        if (producto.getNombre() == null || producto.getNombre().isBlank()) {
            throw new ValidacionException("El nombre del producto es obligatorio.");
        }
        if (producto.getNombre().length() > 100
            || !PATRON_NOMBRE.matcher(producto.getNombre()).matches()) {
            throw new ValidacionException("El nombre del producto contiene caracteres no válidos.");
        }
        BigDecimal precio = producto.getPrecio();
        if (precio == null || precio.signum() < 0) {
            throw new ValidacionException("El precio debe ser un importe mayor o igual que cero.");
        }
        if (precio.scale() > 2 || precio.precision() - precio.scale() > 8) {
            throw new ValidacionException("El precio admite hasta 8 enteros y 2 decimales.");
        }
        if (producto.getExistencia() < 0) {
            throw new ValidacionException("La existencia no puede ser negativa.");
        }
    }
}

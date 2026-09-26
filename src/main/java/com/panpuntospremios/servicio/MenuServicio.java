package com.panpuntospremios.servicio;

import com.panpuntospremios.dao.MenuDAO;
import com.panpuntospremios.enums.Categoria;
import com.panpuntospremios.excepcion.ValidacionException;
import com.panpuntospremios.modelo.Menu;
import com.panpuntospremios.modelo.Producto;
import com.panpuntospremios.util.Transaccion;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class MenuServicio {
    private static final Pattern PATRON_CODIGO = Pattern.compile("^[A-Z0-9][A-Z0-9_-]{0,19}$");

    private final Connection conexion;
    private final MenuDAO menuDAO;

    public MenuServicio(Connection conexion, MenuDAO menuDAO) {
        this.conexion = conexion;
        this.menuDAO = menuDAO;
    }

    public void registrar(Menu menu) throws SQLException {
        validar(menu, true);
        if (menuDAO.buscarPorCodigo(menu.getCodigo()) != null) {
            throw new ValidacionException("Ya existe un menú con ese código.");
        }
        Transaccion.ejecutar(conexion, () -> {
            menuDAO.registrar(menu);
            return null;
        });
    }

    public List<Menu> listarActivos() throws SQLException {
        return menuDAO.listarActivos();
    }

    public void actualizar(Menu menu) throws SQLException {
        validar(menu, false);
        if (menu.getIdMenu() <= 0) {
            throw new ValidacionException("El ID del menú debe ser mayor que cero.");
        }
        Transaccion.ejecutar(conexion, () -> {
            menuDAO.actualizar(menu);
            return null;
        });
    }

    public void desactivar(int idMenu) throws SQLException {
        if (idMenu <= 0) {
            throw new ValidacionException("El ID del menú debe ser mayor que cero.");
        }
        menuDAO.desactivar(idMenu);
    }

    private void validar(Menu menu, boolean validarCodigo) {
        if (menu == null) {
            throw new ValidacionException("Los datos del menú son obligatorios.");
        }
        if (validarCodigo
            && (menu.getCodigo() == null || !PATRON_CODIGO.matcher(menu.getCodigo()).matches())) {
            throw new ValidacionException(
                "El código debe tener de 1 a 20 caracteres en mayúsculas, números, _ o -."
            );
        }
        if (menu.getNombre() == null || menu.getNombre().isBlank() || menu.getNombre().length() > 100) {
            throw new ValidacionException("El nombre del menú es obligatorio y admite hasta 100 caracteres.");
        }
        BigDecimal precio = menu.getPrecio();
        if (precio == null || precio.signum() < 0 || precio.scale() > 2
            || precio.precision() - precio.scale() > 8) {
            throw new ValidacionException("El precio admite hasta 8 enteros, 2 decimales y no puede ser negativo.");
        }
        validarComponentes(menu.getComponentes());
    }

    private void validarComponentes(List<Producto> componentes) {
        if (componentes == null || componentes.size() != 3) {
            throw new ValidacionException(
                "El menú debe contener exactamente un sándwich, una bebida y un acompañamiento."
            );
        }
        EnumMap<Categoria, Integer> cantidades = new EnumMap<>(Categoria.class);
        Set<Integer> ids = new HashSet<>();
        for (Producto producto : componentes) {
            if (producto == null || producto.getIdProducto() <= 0 || !producto.isActivo()) {
                throw new ValidacionException("Todos los componentes deben ser productos activos guardados.");
            }
            if (!ids.add(producto.getIdProducto())) {
                throw new ValidacionException("Un producto no puede repetirse dentro del menú.");
            }
            Categoria categoria;
            try {
                categoria = Categoria.valueOf(producto.getCategoria());
            } catch (Exception e) {
                throw new ValidacionException("Un componente tiene una categoría no válida.");
            }
            cantidades.merge(categoria, 1, Integer::sum);
        }
        for (Categoria categoria : Categoria.values()) {
            if (cantidades.getOrDefault(categoria, 0) != 1) {
                throw new ValidacionException(
                    "El menú debe contener exactamente un sándwich, una bebida y un acompañamiento."
                );
            }
        }
    }
}

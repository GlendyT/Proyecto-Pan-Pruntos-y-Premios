package com.panpuntospremios.vista;

import com.panpuntospremios.dao.MenuDAOImpl;
import com.panpuntospremios.dao.ProductoDAOImpl;
import com.panpuntospremios.enums.Categoria;
import com.panpuntospremios.excepcion.ValidacionException;
import com.panpuntospremios.modelo.Menu;
import com.panpuntospremios.modelo.Producto;
import com.panpuntospremios.servicio.MenuServicio;
import com.panpuntospremios.servicio.ProductoServicio;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FormMenus extends FormularioAsincrono {
    private final DefaultTableModel modeloTabla;
    private final JTable tabla;
    private final JButton btnRegistrar;
    private final JButton btnEditar;
    private final JButton btnDesactivar;
    private final JButton btnActualizar;
    private MenuServicio menuServicio;
    private ProductoServicio productoServicio;
    private List<Menu> menus = List.of();
    private List<Producto> productos = List.of();

    public FormMenus() {
        setTitle("Gestión de Menús");
        setSize(1000, 520);
        setLocationRelativeTo(null);

        modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Código", "Nombre", "Precio", "Componentes"},
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabla = new JTable(modeloTabla);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        btnRegistrar = new JButton("Registrar");
        btnEditar = new JButton("Editar");
        btnDesactivar = new JButton("Desactivar");
        btnActualizar = new JButton("Actualizar tabla");
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(btnActualizar);
        botones.add(btnRegistrar);
        botones.add(btnEditar);
        botones.add(btnDesactivar);

        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);

        btnRegistrar.addActionListener(e -> registrar());
        btnEditar.addActionListener(e -> editar());
        btnDesactivar.addActionListener(e -> desactivar());
        btnActualizar.addActionListener(e -> cargar());
        habilitarControles(false);
        inicializar();
    }

    private void inicializar() {
        ejecutar(() -> {
            Connection conexion = abrirConexion();
            ProductoServicio nuevoProductoServicio = new ProductoServicio(new ProductoDAOImpl(conexion));
            MenuServicio nuevoMenuServicio = new MenuServicio(conexion, new MenuDAOImpl(conexion));
            return new DatosIniciales(
                nuevoMenuServicio,
                nuevoProductoServicio,
                nuevoMenuServicio.listarActivos(),
                nuevoProductoServicio.listarActivos()
            );
        }, datos -> {
            menuServicio = datos.menuServicio();
            productoServicio = datos.productoServicio();
            actualizarDatos(datos.menus(), datos.productos());
        }, "No se pudieron cargar los menús.");
    }

    private void cargar() {
        ejecutar(
            () -> new Datos(menuServicio.listarActivos(), productoServicio.listarActivos()),
            datos -> actualizarDatos(datos.menus(), datos.productos()),
            "No se pudieron cargar los menús."
        );
    }

    private void registrar() {
        DialogoMenu dialogo = crearDialogo(null);
        if (dialogo == null) {
            return;
        }
        Menu menu;
        try {
            menu = dialogo.crearMenu(0, null);
        } catch (RuntimeException e) {
            mostrarValidacion(e.getMessage());
            return;
        }
        ejecutar(() -> {
            menuServicio.registrar(menu);
            return menuServicio.listarActivos();
        }, lista -> {
            actualizarDatos(lista, productos);
            JOptionPane.showMessageDialog(this, "Menú registrado correctamente.");
        }, "No se pudo registrar el menú.");
    }

    private void editar() {
        Menu seleccionado = menuSeleccionado();
        if (seleccionado == null) {
            mostrarValidacion("Selecciona un menú.");
            return;
        }
        DialogoMenu dialogo = crearDialogo(seleccionado);
        if (dialogo == null) {
            return;
        }
        Menu menu;
        try {
            menu = dialogo.crearMenu(seleccionado.getIdMenu(), seleccionado.getCodigo());
        } catch (RuntimeException e) {
            mostrarValidacion(e.getMessage());
            return;
        }
        ejecutar(() -> {
            menuServicio.actualizar(menu);
            return menuServicio.listarActivos();
        }, lista -> {
            actualizarDatos(lista, productos);
            JOptionPane.showMessageDialog(this, "Menú actualizado correctamente.");
        }, "No se pudo actualizar el menú.");
    }

    private void desactivar() {
        Menu seleccionado = menuSeleccionado();
        if (seleccionado == null) {
            mostrarValidacion("Selecciona un menú.");
            return;
        }
        if (JOptionPane.showConfirmDialog(
            this,
            "¿Desactivar el menú " + seleccionado.getNombre() + "?",
            "Confirmar",
            JOptionPane.YES_NO_OPTION
        ) != JOptionPane.YES_OPTION) {
            return;
        }
        ejecutar(() -> {
            menuServicio.desactivar(seleccionado.getIdMenu());
            return menuServicio.listarActivos();
        }, lista -> {
            actualizarDatos(lista, productos);
            JOptionPane.showMessageDialog(this, "Menú desactivado correctamente.");
        }, "No se pudo desactivar el menú.");
    }

    private DialogoMenu crearDialogo(Menu menu) {
        List<Producto> sandwiches = filtrar(Categoria.SANDWICH);
        List<Producto> bebidas = filtrar(Categoria.BEBIDA);
        List<Producto> acompanamientos = filtrar(Categoria.ACOMPANAMIENTO);
        if (sandwiches.isEmpty() || bebidas.isEmpty() || acompanamientos.isEmpty()) {
            mostrarValidacion(
                "Se necesita al menos un producto activo de cada categoría para crear un menú."
            );
            return null;
        }

        JTextField codigo = new JTextField(menu == null ? "" : menu.getCodigo());
        codigo.setEnabled(menu == null);
        JTextField nombre = new JTextField(menu == null ? "" : menu.getNombre());
        JTextField precio = new JTextField(menu == null ? "" : menu.getPrecio().toPlainString());
        JComboBox<Producto> sandwich = combo(sandwiches);
        JComboBox<Producto> bebida = combo(bebidas);
        JComboBox<Producto> acompanamiento = combo(acompanamientos);
        if (menu != null) {
            seleccionarComponente(sandwich, menu, Categoria.SANDWICH);
            seleccionarComponente(bebida, menu, Categoria.BEBIDA);
            seleccionarComponente(acompanamiento, menu, Categoria.ACOMPANAMIENTO);
        }
        Object[] campos = {
            "Código:", codigo,
            "Nombre:", nombre,
            "Precio:", precio,
            "Sándwich:", sandwich,
            "Bebida:", bebida,
            "Acompañamiento:", acompanamiento
        };
        int resultado = JOptionPane.showConfirmDialog(
            this,
            campos,
            menu == null ? "Registrar menú" : "Editar menú",
            JOptionPane.OK_CANCEL_OPTION
        );
        return resultado == JOptionPane.OK_OPTION
            ? new DialogoMenu(codigo, nombre, precio, sandwich, bebida, acompanamiento)
            : null;
    }

    private JComboBox<Producto> combo(List<Producto> opciones) {
        JComboBox<Producto> combo = new JComboBox<>(opciones.toArray(Producto[]::new));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus
            ) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Producto producto) {
                    setText(producto.getCodigo() + " - " + producto.getNombre());
                }
                return this;
            }
        });
        return combo;
    }

    private void seleccionarComponente(JComboBox<Producto> combo, Menu menu, Categoria categoria) {
        menu.getComponentes().stream()
            .filter(producto -> categoria.name().equals(producto.getCategoria()))
            .findFirst()
            .ifPresent(encontrado -> {
                for (int i = 0; i < combo.getItemCount(); i++) {
                    if (combo.getItemAt(i).getIdProducto() == encontrado.getIdProducto()) {
                        combo.setSelectedIndex(i);
                        break;
                    }
                }
            });
    }

    private List<Producto> filtrar(Categoria categoria) {
        return productos.stream()
            .filter(producto -> categoria.name().equals(producto.getCategoria()))
            .toList();
    }

    private Menu menuSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            return null;
        }
        int id = ((Number) modeloTabla.getValueAt(fila, 0)).intValue();
        return menus.stream().filter(menu -> menu.getIdMenu() == id).findFirst().orElse(null);
    }

    private void actualizarDatos(List<Menu> nuevosMenus, List<Producto> nuevosProductos) {
        menus = new ArrayList<>(nuevosMenus);
        productos = new ArrayList<>(nuevosProductos);
        modeloTabla.setRowCount(0);
        for (Menu menu : menus) {
            String componentes = menu.getComponentes().stream()
                .map(Producto::getNombre)
                .collect(Collectors.joining(" + "));
            modeloTabla.addRow(new Object[]{
                menu.getIdMenu(), menu.getCodigo(), menu.getNombre(), menu.getPrecio(), componentes
            });
        }
    }

    private BigDecimal decimal(String valor) {
        try {
            return new BigDecimal(valor.trim());
        } catch (Exception e) {
            throw new ValidacionException("El precio debe ser un número válido.");
        }
    }

    private void mostrarValidacion(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Validación", JOptionPane.WARNING_MESSAGE);
    }

    @Override
    protected void habilitarControles(boolean habilitar) {
        boolean disponible = habilitar && menuServicio != null;
        tabla.setEnabled(disponible);
        btnRegistrar.setEnabled(disponible);
        btnEditar.setEnabled(disponible);
        btnDesactivar.setEnabled(disponible);
        btnActualizar.setEnabled(disponible);
    }

    private record DatosIniciales(
        MenuServicio menuServicio,
        ProductoServicio productoServicio,
        List<Menu> menus,
        List<Producto> productos
    ) {}

    private record Datos(List<Menu> menus, List<Producto> productos) {}

    private record DialogoMenu(
        JTextField codigo,
        JTextField nombre,
        JTextField precio,
        JComboBox<Producto> sandwich,
        JComboBox<Producto> bebida,
        JComboBox<Producto> acompanamiento
    ) {
        private Menu crearMenu(int idMenu, String codigoExistente) {
            BigDecimal importe;
            try {
                importe = new BigDecimal(precio.getText().trim());
            } catch (Exception e) {
                throw new ValidacionException("El precio debe ser un número válido.");
            }
            Menu menu = new Menu(
                idMenu,
                codigoExistente == null ? codigo.getText().trim().toUpperCase() : codigoExistente,
                nombre.getText().trim(),
                importe,
                true
            );
            menu.setComponentes(List.of(
                (Producto) sandwich.getSelectedItem(),
                (Producto) bebida.getSelectedItem(),
                (Producto) acompanamiento.getSelectedItem()
            ));
            return menu;
        }
    }
}

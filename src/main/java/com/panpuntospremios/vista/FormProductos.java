package com.panpuntospremios.vista;

import com.panpuntospremios.dao.ProductoDAOImpl;
import com.panpuntospremios.enums.Categoria;
import com.panpuntospremios.excepcion.ValidacionException;
import com.panpuntospremios.modelo.Producto;
import com.panpuntospremios.servicio.ProductoServicio;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

public class FormProductos extends FormularioAsincrono {
    private final DefaultTableModel modeloTabla;
    private final JTable tabla;
    private final JButton btnRegistrar;
    private final JButton btnEditar;
    private final JButton btnDesactivar;
    private final JButton btnActualizar;
    private ProductoServicio servicio;

    public FormProductos() {
        setTitle("Gestión de Productos");
        setSize(850, 500);
        setLocationRelativeTo(null);

        modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Código", "Categoría", "Nombre", "Precio", "Existencia"},
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
            ProductoServicio nuevoServicio = new ProductoServicio(new ProductoDAOImpl(conexion));
            return new DatosIniciales(nuevoServicio, nuevoServicio.listarActivos());
        }, datos -> {
            servicio = datos.servicio();
            mostrar(datos.productos());
        }, "No se pudieron cargar los productos.");
    }

    private void cargar() {
        ejecutar(() -> servicio.listarActivos(), this::mostrar, "No se pudieron cargar los productos.");
    }

    private void registrar() {
        JTextField codigo = new JTextField();
        JComboBox<Categoria> categoria = new JComboBox<>(Categoria.values());
        JTextField nombre = new JTextField();
        JTextField precio = new JTextField();
        Object[] campos = {
            "Código:", codigo,
            "Categoría:", categoria,
            "Nombre:", nombre,
            "Precio:", precio
        };
        if (JOptionPane.showConfirmDialog(this, campos, "Registrar producto", JOptionPane.OK_CANCEL_OPTION)
            != JOptionPane.OK_OPTION) {
            return;
        }
        Producto producto;
        try {
            producto = new Producto(
                0,
                codigo.getText().trim().toUpperCase(),
                ((Categoria) categoria.getSelectedItem()).name(),
                nombre.getText().trim(),
                decimal(precio.getText()),
                0,
                true
            );
        } catch (RuntimeException e) {
            mostrarValidacion(e.getMessage());
            return;
        }
        ejecutar(() -> {
            servicio.registrar(producto);
            return servicio.listarActivos();
        }, productos -> {
            mostrar(productos);
            JOptionPane.showMessageDialog(this, "Producto registrado correctamente.");
        }, "No se pudo registrar el producto.");
    }

    private void editar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            mostrarValidacion("Selecciona un producto.");
            return;
        }
        JTextField nombre = new JTextField((String) modeloTabla.getValueAt(fila, 3));
        JTextField precio = new JTextField(modeloTabla.getValueAt(fila, 4).toString());
        Object[] campos = {"Nombre:", nombre, "Precio:", precio};
        if (JOptionPane.showConfirmDialog(this, campos, "Editar producto", JOptionPane.OK_CANCEL_OPTION)
            != JOptionPane.OK_OPTION) {
            return;
        }
        Producto producto;
        try {
            producto = new Producto(
                ((Number) modeloTabla.getValueAt(fila, 0)).intValue(),
                (String) modeloTabla.getValueAt(fila, 1),
                (String) modeloTabla.getValueAt(fila, 2),
                nombre.getText().trim(),
                decimal(precio.getText()),
                ((Number) modeloTabla.getValueAt(fila, 5)).intValue(),
                true
            );
        } catch (RuntimeException e) {
            mostrarValidacion(e.getMessage());
            return;
        }
        ejecutar(() -> {
            servicio.actualizar(producto);
            return servicio.listarActivos();
        }, productos -> {
            mostrar(productos);
            JOptionPane.showMessageDialog(this, "Producto actualizado correctamente.");
        }, "No se pudo actualizar el producto.");
    }

    private void desactivar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            mostrarValidacion("Selecciona un producto.");
            return;
        }
        String nombre = (String) modeloTabla.getValueAt(fila, 3);
        if (JOptionPane.showConfirmDialog(
            this,
            "¿Desactivar el producto " + nombre + "?",
            "Confirmar",
            JOptionPane.YES_NO_OPTION
        ) != JOptionPane.YES_OPTION) {
            return;
        }
        int id = ((Number) modeloTabla.getValueAt(fila, 0)).intValue();
        ejecutar(() -> {
            servicio.desactivar(id);
            return servicio.listarActivos();
        }, productos -> {
            mostrar(productos);
            JOptionPane.showMessageDialog(this, "Producto desactivado correctamente.");
        }, "No se pudo desactivar el producto.");
    }

    private BigDecimal decimal(String valor) {
        try {
            return new BigDecimal(valor.trim());
        } catch (Exception e) {
            throw new ValidacionException("El precio debe ser un número válido.");
        }
    }

    private void mostrar(List<Producto> productos) {
        modeloTabla.setRowCount(0);
        for (Producto p : productos) {
            modeloTabla.addRow(new Object[]{
                p.getIdProducto(), p.getCodigo(), p.getCategoria(), p.getNombre(),
                p.getPrecio(), p.getExistencia()
            });
        }
    }

    private void mostrarValidacion(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Validación", JOptionPane.WARNING_MESSAGE);
    }

    @Override
    protected void habilitarControles(boolean habilitar) {
        boolean disponible = habilitar && servicio != null;
        tabla.setEnabled(disponible);
        btnRegistrar.setEnabled(disponible);
        btnEditar.setEnabled(disponible);
        btnDesactivar.setEnabled(disponible);
        btnActualizar.setEnabled(disponible);
    }

    private record DatosIniciales(ProductoServicio servicio, List<Producto> productos) {}
}

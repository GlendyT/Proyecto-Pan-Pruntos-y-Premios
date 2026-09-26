package com.panpuntospremios.vista;

import com.panpuntospremios.dao.MovimientoInventarioDAOImpl;
import com.panpuntospremios.dao.ProductoDAOImpl;
import com.panpuntospremios.excepcion.ValidacionException;
import com.panpuntospremios.modelo.MovimientoInventario;
import com.panpuntospremios.modelo.Producto;
import com.panpuntospremios.servicio.InventarioServicio;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FormInventario extends FormularioAsincrono {
    private final DefaultTableModel modeloProductos;
    private final DefaultTableModel modeloMovimientos;
    private final JTable tablaProductos;
    private final JTable tablaMovimientos;
    private final JButton btnAbastecer;
    private final JButton btnActualizar;
    private InventarioServicio servicio;

    public FormInventario() {
        setTitle("Inventario");
        setSize(1000, 650);
        setLocationRelativeTo(null);

        modeloProductos = noEditable(new Object[]{"ID", "Código", "Categoría", "Producto", "Existencia"});
        modeloMovimientos = noEditable(
            new Object[]{"ID", "Fecha", "Producto", "Tipo", "Cantidad", "Existencia resultante"}
        );
        tablaProductos = new JTable(modeloProductos);
        tablaProductos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaMovimientos = new JTable(modeloMovimientos);

        JSplitPane panelCentral = new JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            panelConTitulo("Existencias actuales", tablaProductos),
            panelConTitulo("Historial de movimientos", tablaMovimientos)
        );
        panelCentral.setResizeWeight(0.42);

        btnAbastecer = new JButton("Abastecer producto");
        btnActualizar = new JButton("Actualizar");
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(btnActualizar);
        botones.add(btnAbastecer);

        add(panelCentral, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);

        btnAbastecer.addActionListener(e -> abastecer());
        btnActualizar.addActionListener(e -> cargar());
        habilitarControles(false);
        inicializar();
    }

    private void inicializar() {
        ejecutar(() -> {
            Connection conexion = abrirConexion();
            InventarioServicio nuevoServicio = new InventarioServicio(
                conexion,
                new ProductoDAOImpl(conexion),
                new MovimientoInventarioDAOImpl(conexion)
            );
            return new DatosIniciales(nuevoServicio, cargarDatos(nuevoServicio));
        }, datos -> {
            servicio = datos.servicio();
            mostrar(datos.datos());
        }, "No se pudo cargar el inventario.");
    }

    private void cargar() {
        ejecutar(() -> cargarDatos(servicio), this::mostrar, "No se pudo cargar el inventario.");
    }

    private Datos cargarDatos(InventarioServicio servicioActual) throws Exception {
        return new Datos(servicioActual.listarProductos(), servicioActual.listarMovimientos());
    }

    private void abastecer() {
        int fila = tablaProductos.getSelectedRow();
        if (fila < 0) {
            mostrarValidacion("Selecciona un producto.");
            return;
        }
        String valor = JOptionPane.showInputDialog(
            this,
            "Cantidad a agregar a " + modeloProductos.getValueAt(fila, 3) + ":",
            "Abastecer",
            JOptionPane.QUESTION_MESSAGE
        );
        if (valor == null) {
            return;
        }
        int cantidad;
        try {
            cantidad = Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            mostrarValidacion("La cantidad debe ser un número entero.");
            return;
        }
        int idProducto = ((Number) modeloProductos.getValueAt(fila, 0)).intValue();
        ejecutar(() -> {
            servicio.abastecer(idProducto, cantidad);
            return cargarDatos(servicio);
        }, datos -> {
            mostrar(datos);
            JOptionPane.showMessageDialog(this, "Abastecimiento registrado correctamente.");
        }, "No se pudo abastecer el producto.");
    }

    private void mostrar(Datos datos) {
        modeloProductos.setRowCount(0);
        Map<Integer, String> nombres = new HashMap<>();
        for (Producto producto : datos.productos()) {
            nombres.put(producto.getIdProducto(), producto.getCodigo() + " - " + producto.getNombre());
            modeloProductos.addRow(new Object[]{
                producto.getIdProducto(),
                producto.getCodigo(),
                producto.getCategoria(),
                producto.getNombre(),
                producto.getExistencia()
            });
        }

        modeloMovimientos.setRowCount(0);
        SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (MovimientoInventario movimiento : datos.movimientos()) {
            String fecha = movimiento.getFecha() == null ? "" : formato.format(movimiento.getFecha());
            modeloMovimientos.addRow(new Object[]{
                movimiento.getIdMovimiento(),
                fecha,
                nombres.getOrDefault(movimiento.getIdProducto(), "ID " + movimiento.getIdProducto()),
                movimiento.getTipo(),
                movimiento.getCantidad(),
                movimiento.getExistenciaResultante()
            });
        }
    }

    private JPanel panelConTitulo(String titulo, JTable tabla) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(titulo));
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);
        return panel;
    }

    private DefaultTableModel noEditable(Object[] columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void mostrarValidacion(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Validación", JOptionPane.WARNING_MESSAGE);
    }

    @Override
    protected void habilitarControles(boolean habilitar) {
        boolean disponible = habilitar && servicio != null;
        tablaProductos.setEnabled(disponible);
        tablaMovimientos.setEnabled(disponible);
        btnAbastecer.setEnabled(disponible);
        btnActualizar.setEnabled(disponible);
    }

    private record DatosIniciales(InventarioServicio servicio, Datos datos) {}

    private record Datos(List<Producto> productos, List<MovimientoInventario> movimientos) {}
}

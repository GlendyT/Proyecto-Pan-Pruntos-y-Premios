package com.panpuntospremios.vista;

import com.panpuntospremios.dao.*;
import com.panpuntospremios.modelo.*;
import com.panpuntospremios.modelo.Menu;
import com.panpuntospremios.servicio.*;
import com.panpuntospremios.util.ConexionOracle;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class FormPedido extends FormularioAsincrono {
    private final List<ItemVenta> items = new ArrayList<>();
    private final JTextField identificador = new JTextField(12);
    private final JLabel clienteTexto = new JLabel("Sin cliente seleccionado");
    private final JComboBox<Object> catalogo = new JComboBox<>();
    private final JSpinner cantidad = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
    private final JLabel total = new JLabel("Total: Q0.00 | Puntos: 0");
    private final DefaultTableModel modelo = new DefaultTableModel(
        new Object[]{"Nombre", "Cantidad", "Precio", "Subtotal", "Puntos"}, 0) {
        public boolean isCellEditable(int fila, int columna) { return false; }
    };
    private final JTable tabla = new JTable(modelo);
    private Cliente cliente;

    public FormPedido() {
        setTitle("Nuevo pedido"); setSize(950, 550); setLocationRelativeTo(null);
        JPanel norte = new JPanel(new GridLayout(2, 1));
        JPanel clientePanel = new JPanel();
        JButton buscar = new JButton("Buscar cliente");
        clientePanel.add(new JLabel("Identificador:")); clientePanel.add(identificador);
        clientePanel.add(buscar); clientePanel.add(clienteTexto); norte.add(clientePanel);
        JPanel seleccion = new JPanel();
        JButton cargar = new JButton("Actualizar catálogo");
        JButton agregar = new JButton("Agregar");
        seleccion.add(catalogo); seleccion.add(cantidad); seleccion.add(agregar); seleccion.add(cargar);
        norte.add(seleccion); add(norte, BorderLayout.NORTH);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        JPanel sur = new JPanel();
        JButton editar = new JButton("Cambiar cantidad");
        JButton quitar = new JButton("Quitar");
        JButton confirmar = new JButton("Confirmar pedido");
        sur.add(total); sur.add(editar); sur.add(quitar); sur.add(confirmar); add(sur, BorderLayout.SOUTH);
        buscar.addActionListener(e -> buscarCliente());
        cargar.addActionListener(e -> cargarCatalogo());
        agregar.addActionListener(e -> {
            Object seleccionado = catalogo.getSelectedItem();
            if (seleccionado == null) return;
            if (!confirmarCantidad()) return;
            items.add(crearItem(seleccionado, (Integer)cantidad.getValue())); actualizar();
        });
        quitar.addActionListener(e -> { int fila = tabla.getSelectedRow(); if (fila >= 0) { items.remove(fila); actualizar(); } });
        editar.addActionListener(e -> {
            int fila = tabla.getSelectedRow();
            if (fila < 0 || !confirmarCantidad()) return;
            ItemVenta anterior = items.get(fila);
            Object producto = anterior instanceof ProductoIndividual pi ? pi.getProducto() : ((MenuCompleto)anterior).getMenu();
            items.set(fila, crearItem(producto, (Integer)cantidad.getValue())); actualizar();
        });
        confirmar.addActionListener(e -> confirmar());
        cargarCatalogo();
    }

    private boolean confirmarCantidad() {
        try { cantidad.commitEdit(); return true; }
        catch (java.text.ParseException e) { JOptionPane.showMessageDialog(this, "Cantidad inválida."); return false; }
    }

    private ItemVenta crearItem(Object objeto, int unidades) {
        return objeto instanceof Producto p ? new ProductoIndividual(p, unidades) : new MenuCompleto((Menu)objeto, unidades);
    }

    private void buscarCliente() {
        String texto = identificador.getText().trim();
        cliente = null; clienteTexto.setText("Sin cliente seleccionado");
        ejecutar(() -> {
            try (Connection c = ConexionOracle.obtener()) { return new ClienteDAOImpl(c).buscarPorIdentificador(texto); }
        }, encontrado -> {
            if (encontrado == null || !encontrado.isActivo()) {
                JOptionPane.showMessageDialog(this, "No se encontró un cliente activo."); return;
            }
            cliente = encontrado; clienteTexto.setText(cliente.getNombre());
        }, "No se pudo buscar el cliente.");
    }

    private void cargarCatalogo() {
        ejecutar(() -> {
            try (Connection c = ConexionOracle.obtener()) {
                List<Object> resultado = new ArrayList<>(new ProductoDAOImpl(c).listarActivos());
                resultado.addAll(new MenuDAOImpl(c).listarActivos()); return resultado;
            }
        }, resultado -> { catalogo.removeAllItems(); resultado.forEach(catalogo::addItem); }, "No se pudo cargar el catálogo.");
    }

    private void actualizar() {
        modelo.setRowCount(0); BigDecimal importe = BigDecimal.ZERO; int puntos = 0;
        for (ItemVenta item : items) {
            String nombre = item instanceof ProductoIndividual pi ? pi.getProducto().getNombre() : ((MenuCompleto)item).getMenu().getNombre();
            modelo.addRow(new Object[]{nombre, item.getCantidad(), item.getPrecioUnitario(), item.calcularSubtotal(), item.calcularPuntos()});
            importe = importe.add(item.calcularSubtotal()); puntos += item.calcularPuntos();
        }
        total.setText("Total: Q" + importe + " | Puntos: " + puntos);
    }

    private void confirmar() {
        if (cliente == null || items.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selecciona un cliente y agrega al menos un ítem."); return;
        }
        Pedido pedido = new Pedido(0, cliente.getIdCliente(), null, "PENDIENTE", BigDecimal.ZERO, 0, null, null);
        List<ItemVenta> copia = List.copyOf(items);
        ejecutar(() -> {
            try (Connection c = ConexionOracle.obtener()) {
                return new PedidoServicio(new PedidoDAOImpl(c), new ProductoDAOImpl(c), c).crearPedido(pedido, copia);
            }
        }, id -> {
            JOptionPane.showMessageDialog(this, "Pedido #" + id + " creado. Total: Q" + pedido.getTotal() + " | Puntos previstos: " + pedido.getPuntosGenerados());
            dispose();
        }, "No se pudo crear el pedido.");
    }

    @Override protected void habilitarControles(boolean habilitar) { habilitar(getContentPane(), habilitar); }
    static void habilitar(Container contenedor, boolean habilitar) {
        for (Component componente : contenedor.getComponents()) {
            componente.setEnabled(habilitar);
            if (componente instanceof Container hijo) habilitar(hijo, habilitar);
        }
    }
}

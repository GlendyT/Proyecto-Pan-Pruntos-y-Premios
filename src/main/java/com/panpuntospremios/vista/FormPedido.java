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

public class FormPedido extends JFrame {

    private final DefaultTableModel modeloItems;
    private final List<ItemVenta> items = new ArrayList<>();
    private final JLabel lblTotal = new JLabel("Total: Q0.00");
    private final JLabel lblPuntos = new JLabel("Puntos: 0");
    private final JLabel lblCliente = new JLabel("Sin cliente seleccionado");
    private Cliente clienteSeleccionado;
    private Connection conexion;
    private ClienteServicio clienteServicio;
    private ProductoServicio productoServicio;
    private MenuServicio menuServicio;
    private PedidoServicio pedidoServicio;

    public FormPedido() {
        setTitle("Nuevo Pedido");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        try {
            conexion = ConexionOracle.obtener();
            clienteServicio = new ClienteServicio(new ClienteDAOImpl(conexion));
            productoServicio = new ProductoServicio(new ProductoDAOImpl(conexion));
            menuServicio = new MenuServicio(conexion, new MenuDAOImpl(conexion));
            pedidoServicio = new PedidoServicio(new PedidoDAOImpl(conexion), new ProductoDAOImpl(conexion), conexion);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al conectar con la base de datos.", "Error", JOptionPane.ERROR_MESSAGE);
        }

        // Panel cliente
        JPanel panelCliente = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField txtBuscarCliente = new JTextField(15);
        JButton btnBuscarCliente = new JButton("Buscar cliente");
        panelCliente.add(new JLabel("Identificador:"));
        panelCliente.add(txtBuscarCliente);
        panelCliente.add(btnBuscarCliente);
        panelCliente.add(lblCliente);

        // Panel catálogo
        JPanel panelCatalogo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JComboBox<String> cmbTipo = new JComboBox<>(new String[]{"PRODUCTO", "MENU"});
        JComboBox<Object> cmbItem = new JComboBox<>();
        JSpinner spnCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        JButton btnAgregar = new JButton("Agregar ítem");
        panelCatalogo.add(new JLabel("Tipo:"));
        panelCatalogo.add(cmbTipo);
        panelCatalogo.add(new JLabel("Ítem:"));
        panelCatalogo.add(cmbItem);
        panelCatalogo.add(new JLabel("Cantidad:"));
        panelCatalogo.add(spnCantidad);
        panelCatalogo.add(btnAgregar);

        // Tabla de ítems
        modeloItems = new DefaultTableModel(new Object[]{"Tipo", "Nombre", "Cantidad", "Precio unitario", "Subtotal", "Puntos"}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable tablaItems = new JTable(modeloItems);
        tablaItems.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Panel totales y botones
        JPanel panelSur = new JPanel(new BorderLayout());
        JPanel panelTotales = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblTotal.setFont(new Font("Arial", Font.BOLD, 14));
        lblPuntos.setFont(new Font("Arial", Font.BOLD, 14));
        panelTotales.add(lblTotal);
        panelTotales.add(Box.createHorizontalStrut(20));
        panelTotales.add(lblPuntos);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnEliminar = new JButton("Eliminar ítem");
        JButton btnConfirmar = new JButton("Confirmar pedido");
        panelBotones.add(btnEliminar);
        panelBotones.add(btnConfirmar);

        panelSur.add(panelTotales, BorderLayout.WEST);
        panelSur.add(panelBotones, BorderLayout.EAST);

        // Layout principal
        JPanel panelNorte = new JPanel(new GridLayout(2, 1));
        panelNorte.add(panelCliente);
        panelNorte.add(panelCatalogo);

        setLayout(new BorderLayout());
        add(panelNorte, BorderLayout.NORTH);
        add(new JScrollPane(tablaItems), BorderLayout.CENTER);
        add(panelSur, BorderLayout.SOUTH);

        // Acciones
        btnBuscarCliente.addActionListener(e -> buscarCliente(txtBuscarCliente.getText().trim()));
        cmbTipo.addActionListener(e -> cargarCatalogo(cmbTipo, cmbItem));
        btnAgregar.addActionListener(e -> agregarItem(cmbTipo, cmbItem, (int) spnCantidad.getValue()));
        btnEliminar.addActionListener(e -> eliminarItem(tablaItems.getSelectedRow()));
        btnConfirmar.addActionListener(e -> confirmarPedido());

        cargarCatalogo(cmbTipo, cmbItem);
    }

    private void buscarCliente(String identificador) {
        if (identificador.isBlank()) return;
        try {
            clienteSeleccionado = clienteServicio.buscarPorIdentificador(identificador);
            if (clienteSeleccionado != null) {
                lblCliente.setText(clienteSeleccionado.getNombre() + " | Puntos: " + clienteSeleccionado.getSaldoPuntos());
            } else {
                JOptionPane.showMessageDialog(this, "No se encontró el cliente.", "Sin resultados", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al buscar cliente: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarCatalogo(JComboBox<String> cmbTipo, JComboBox<Object> cmbItem) {
        cmbItem.removeAllItems();
        try {
            if ("PRODUCTO".equals(cmbTipo.getSelectedItem())) {
                for (Producto p : productoServicio.listarActivos()) cmbItem.addItem(p);
            } else {
                for (Menu m : menuServicio.listarActivos()) cmbItem.addItem(m);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al cargar catálogo: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void agregarItem(JComboBox<String> cmbTipo, JComboBox<Object> cmbItem, int cantidad) {
        if (cmbItem.getSelectedItem() == null) return;
        ItemVenta item;
        String nombre;
        if ("PRODUCTO".equals(cmbTipo.getSelectedItem())) {
            Producto p = (Producto) cmbItem.getSelectedItem();
            item = new ProductoIndividual(p, cantidad);
            nombre = p.getNombre();
        } else {
            Menu m = (Menu) cmbItem.getSelectedItem();
            item = new MenuCompleto(m, cantidad);
            nombre = m.getNombre();
        }
        items.add(item);
        modeloItems.addRow(new Object[]{
            cmbTipo.getSelectedItem(), nombre, cantidad,
            item.getPrecioUnitario(), item.calcularSubtotal(), item.calcularPuntos()
        });
        actualizarTotales();
    }

    private void eliminarItem(int fila) {
        if (fila < 0) { JOptionPane.showMessageDialog(this, "Selecciona un ítem de la tabla."); return; }
        items.remove(fila);
        modeloItems.removeRow(fila);
        actualizarTotales();
    }

    private void actualizarTotales() {
        BigDecimal total = items.stream().map(ItemVenta::calcularSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        int puntos = items.stream().mapToInt(ItemVenta::calcularPuntos).sum();
        lblTotal.setText("Total: Q" + total);
        lblPuntos.setText("Puntos: " + puntos);
    }

    private void confirmarPedido() {
        if (clienteSeleccionado == null) { JOptionPane.showMessageDialog(this, "Selecciona un cliente primero."); return; }
        if (items.isEmpty()) { JOptionPane.showMessageDialog(this, "Agrega al menos un ítem al pedido."); return; }

        BigDecimal total = items.stream().map(ItemVenta::calcularSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        int puntos = items.stream().mapToInt(ItemVenta::calcularPuntos).sum();

        Pedido pedido = new Pedido(0, clienteSeleccionado.getIdCliente(), null, "PENDIENTE", total, puntos, null, null);
        try {
            int idPedido = pedidoServicio.crearPedido(pedido, items);
            JOptionPane.showMessageDialog(this, "Pedido #" + idPedido + " creado correctamente.\nTotal: Q" + total + " | Puntos: " + puntos);
            dispose();
        } catch (com.panpuntospremios.excepcion.StockInsuficienteException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Stock insuficiente", JOptionPane.WARNING_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al crear pedido: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

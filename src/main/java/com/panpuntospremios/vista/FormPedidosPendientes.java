package com.panpuntospremios.vista;

import com.panpuntospremios.dao.*;
import com.panpuntospremios.modelo.*;
import com.panpuntospremios.servicio.*;
import com.panpuntospremios.util.ConexionOracle;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.util.List;

public class FormPedidosPendientes extends JFrame {

    private final DefaultTableModel modeloPedidos;
    private final DefaultTableModel modeloDetalle;
    private final JLabel lblCliente = new JLabel("Sin cliente seleccionado");
    private ClienteServicio clienteServicio;
    private PedidoServicio pedidoServicio;
    private Connection conexion;

    public FormPedidosPendientes() {
        setTitle("Pedidos Pendientes");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        try {
            conexion = ConexionOracle.obtener();
            clienteServicio = new ClienteServicio(new ClienteDAOImpl(conexion));
            pedidoServicio = new PedidoServicio(new PedidoDAOImpl(conexion), new ProductoDAOImpl(conexion), conexion);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al conectar con la base de datos.", "Error", JOptionPane.ERROR_MESSAGE);
        }

        // Panel búsqueda cliente
        JPanel panelBuscar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField txtIdentificador = new JTextField(15);
        JButton btnBuscar = new JButton("Buscar cliente");
        panelBuscar.add(new JLabel("Identificador:"));
        panelBuscar.add(txtIdentificador);
        panelBuscar.add(btnBuscar);
        panelBuscar.add(lblCliente);

        // Tabla pedidos
        modeloPedidos = new DefaultTableModel(new Object[]{"ID Pedido", "Fecha", "Total", "Puntos"}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable tablaPedidos = new JTable(modeloPedidos);
        tablaPedidos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Tabla detalle
        modeloDetalle = new DefaultTableModel(new Object[]{"Tipo", "ID Producto/Menú", "Cantidad", "Precio unitario", "Subtotal", "Puntos"}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable tablaDetalle = new JTable(modeloDetalle);

        // Panel botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnAnular = new JButton("Anular pedido");
        panelBotones.add(btnAnular);

        // Layout — tabla pedidos arriba, detalle abajo
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
            new JScrollPane(tablaPedidos), new JScrollPane(tablaDetalle));
        splitPane.setDividerLocation(200);

        setLayout(new BorderLayout());
        add(panelBuscar, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(panelBotones, BorderLayout.SOUTH);

        // Acciones
        btnBuscar.addActionListener(e -> buscarPedidos(txtIdentificador.getText().trim()));
        tablaPedidos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) cargarDetalle(tablaPedidos.getSelectedRow());
        });
        btnAnular.addActionListener(e -> anular(tablaPedidos.getSelectedRow()));
    }

    private void buscarPedidos(String identificador) {
        if (identificador.isBlank()) return;
        try {
            Cliente cliente = clienteServicio.buscarPorIdentificador(identificador);
            if (cliente == null) {
                JOptionPane.showMessageDialog(this, "No se encontró el cliente.", "Sin resultados", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            lblCliente.setText(cliente.getNombre() + " | Puntos: " + cliente.getSaldoPuntos());
            List<Pedido> pedidos = pedidoServicio.listarPendientesPorCliente(cliente.getIdCliente());
            modeloPedidos.setRowCount(0);
            modeloDetalle.setRowCount(0);
            for (Pedido p : pedidos) {
                modeloPedidos.addRow(new Object[]{p.getIdPedido(), p.getFecha(), p.getTotal(), p.getPuntosGenerados()});
            }
            if (pedidos.isEmpty()) {
                JOptionPane.showMessageDialog(this, "El cliente no tiene pedidos pendientes.", "Sin resultados", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarDetalle(int fila) {
        if (fila < 0) return;
        int idPedido = (int) modeloPedidos.getValueAt(fila, 0);
        try {
            List<PedidoDetalle> detalle = pedidoServicio.listarDetalle(idPedido);
            modeloDetalle.setRowCount(0);
            for (PedidoDetalle d : detalle) {
                modeloDetalle.addRow(new Object[]{
                    d.getTipoItem(),
                    d.getIdProducto() != null ? d.getIdProducto() : d.getIdMenu(),
                    d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal(), d.getPuntos()
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al cargar detalle: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void anular(int fila) {
        if (fila < 0) { JOptionPane.showMessageDialog(this, "Selecciona un pedido de la tabla."); return; }
        int idPedido = (int) modeloPedidos.getValueAt(fila, 0);
        int confirmacion = JOptionPane.showConfirmDialog(this, "¿Anular el pedido #" + idPedido + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirmacion != JOptionPane.YES_OPTION) return;
        try {
            pedidoServicio.anular(idPedido);
            JOptionPane.showMessageDialog(this, "Pedido #" + idPedido + " anulado correctamente.");
            modeloPedidos.removeRow(fila);
            modeloDetalle.setRowCount(0);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

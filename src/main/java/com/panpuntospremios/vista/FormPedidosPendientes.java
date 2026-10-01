package com.panpuntospremios.vista;

import com.panpuntospremios.dao.*;
import com.panpuntospremios.modelo.*;
import com.panpuntospremios.servicio.PedidoServicio;
import com.panpuntospremios.util.ConexionOracle;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.util.List;

public class FormPedidosPendientes extends FormularioAsincrono {
    private final JTextField identificador = new JTextField(15);
    private final DefaultTableModel modelo = tablaModelo("Pedido", "Fecha", "Total", "Puntos previstos");
    private final DefaultTableModel detalle = tablaModelo("Tipo", "Producto/menú ID", "Cantidad", "Precio", "Subtotal", "Puntos");
    private final JTable tabla = new JTable(modelo);
    private final JLabel cliente = new JLabel("Sin cliente seleccionado");
    private int idCliente;

    public FormPedidosPendientes() {
        setTitle("Pedidos pendientes"); setSize(950, 550); setLocationRelativeTo(null);
        JPanel norte = new JPanel(); JButton buscar = new JButton("Buscar cliente");
        norte.add(new JLabel("Identificador:")); norte.add(identificador); norte.add(buscar); norte.add(cliente);
        add(norte, BorderLayout.NORTH);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JSplitPane panel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(tabla), new JScrollPane(new JTable(detalle)));
        panel.setDividerLocation(220); add(panel, BorderLayout.CENTER);
        JPanel sur = new JPanel(); JButton ver = new JButton("Ver detalle");
        JButton anular = new JButton("Anular pedido"); JButton cobrar = new JButton("Cobrar pedido");
        sur.add(ver); sur.add(anular); sur.add(cobrar); add(sur, BorderLayout.SOUTH);
        buscar.addActionListener(e -> buscar());
        ver.addActionListener(e -> verDetalle());
        anular.addActionListener(e -> anular());
        cobrar.addActionListener(e -> { Integer id = seleccionado(); if (id != null) new FormCobro(id).setVisible(true); });
        tabla.getSelectionModel().addListSelectionListener(e -> detalle.setRowCount(0));
    }
    static DefaultTableModel tablaModelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) { public boolean isCellEditable(int f, int c) { return false; } };
    }
    private Integer seleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) { JOptionPane.showMessageDialog(this, "Selecciona un pedido."); return null; }
        return (Integer)modelo.getValueAt(fila, 0);
    }
    private record Resultado(Cliente cliente, List<Pedido> pedidos) {}
    private void buscar() {
        String texto = identificador.getText().trim();
        modelo.setRowCount(0); detalle.setRowCount(0); idCliente = 0; cliente.setText("Sin cliente seleccionado");
        ejecutar(() -> {
            try (Connection c = ConexionOracle.obtener()) {
                Cliente encontrado = new ClienteDAOImpl(c).buscarPorIdentificador(texto);
                return new Resultado(encontrado, encontrado == null ? List.of() : new PedidoDAOImpl(c).listarPendientesPorCliente(encontrado.getIdCliente()));
            }
        }, resultado -> {
            if (resultado.cliente() == null) { JOptionPane.showMessageDialog(this, "No se encontró el cliente."); return; }
            idCliente = resultado.cliente().getIdCliente(); cliente.setText(resultado.cliente().getNombre());
            mostrar(resultado.pedidos());
        }, "No se pudieron buscar los pedidos.");
    }
    private void mostrar(List<Pedido> pedidos) {
        modelo.setRowCount(0); detalle.setRowCount(0);
        for (Pedido p : pedidos) modelo.addRow(new Object[]{p.getIdPedido(), p.getFecha(), p.getTotal(), p.getPuntosGenerados()});
    }
    private void verDetalle() {
        Integer id = seleccionado(); if (id == null) return;
        ejecutar(() -> {
            try (Connection c = ConexionOracle.obtener()) { return new PedidoDAOImpl(c).listarDetalle(id); }
        }, filas -> {
            detalle.setRowCount(0);
            for (PedidoDetalle d : filas) detalle.addRow(new Object[]{d.getTipoItem(), d.getIdProducto() != null ? d.getIdProducto() : d.getIdMenu(), d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal(), d.getPuntos()});
        }, "No se pudo cargar el detalle.");
    }
    private void anular() {
        Integer id = seleccionado(); if (id == null) return;
        if (JOptionPane.showConfirmDialog(this, "¿Anular el pedido #" + id + "?", "Anular", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        int clienteActual = idCliente;
        ejecutar(() -> {
            try (Connection c = ConexionOracle.obtener()) {
                new PedidoServicio(new PedidoDAOImpl(c), new ProductoDAOImpl(c), c).anular(id);
                return new PedidoDAOImpl(c).listarPendientesPorCliente(clienteActual);
            }
        }, this::mostrar, "No se pudo anular el pedido.");
    }
    @Override protected void habilitarControles(boolean habilitar) { FormPedido.habilitar(getContentPane(), habilitar); }
}

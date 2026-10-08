package com.panpuntospremios.vista;

import com.panpuntospremios.dao.PedidoDAOImpl;
import com.panpuntospremios.enums.MetodoPago;
import com.panpuntospremios.modelo.*;
import com.panpuntospremios.servicio.VentaServicio;
import com.panpuntospremios.util.ConexionOracle;
import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

public class FormCobro extends FormularioAsincrono {
    private final JTextField idTexto = new JTextField(8);
    private final JComboBox<MetodoPago> metodo = new JComboBox<>(MetodoPago.values());
    private final JTextField recibido = new JTextField(10);
    private final JComboBox<String> tarjeta = new JComboBox<>(new String[]{"Aprobada", "Rechazada"});
    private final JTextArea comprobante = new JTextArea();
    private final JButton cobrar = new JButton("Cobrar");
    private final JButton entregar = new JButton("Marcar entregado");
    private Pedido pedido;

    public FormCobro() { this(0); }
    public FormCobro(int id) {
        setTitle("Cobro y comprobantes"); setSize(850, 600); setLocationRelativeTo(null);
        JPanel norte = new JPanel(); JButton cargar = new JButton("Consultar pedido");
        norte.add(new JLabel("Pedido #")); norte.add(idTexto); norte.add(cargar); add(norte, BorderLayout.NORTH);
        comprobante.setEditable(false); comprobante.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        add(new JScrollPane(comprobante), BorderLayout.CENTER);
        JPanel sur = new JPanel(new GridLayout(2, 1));
        JPanel pago = new JPanel();
        pago.add(new JLabel("Método:")); pago.add(metodo); pago.add(new JLabel("Efectivo recibido: Q")); pago.add(recibido);
        pago.add(new JLabel("Tarjeta (simulación):")); pago.add(tarjeta); sur.add(pago);
        JPanel acciones = new JPanel(); acciones.add(cobrar); acciones.add(entregar); sur.add(acciones); add(sur, BorderLayout.SOUTH);
        cargar.addActionListener(e -> cargar());
        cobrar.addActionListener(e -> cobrar());
        entregar.addActionListener(e -> entregar());
        metodo.addActionListener(e -> habilitarPago());
        habilitarControles(true);
        if (id > 0) { idTexto.setText(Integer.toString(id)); cargar(); }
    }
    private record Consulta(Pedido pedido, List<PedidoDetalle> detalle, String comprobante) {}
    private void cargar() {
        final int id;
        try { id = Integer.parseInt(idTexto.getText().trim()); if (id <= 0) throw new NumberFormatException(); }
        catch (NumberFormatException e) { JOptionPane.showMessageDialog(this, "Introduce un ID de pedido positivo."); return; }
        pedido = null; comprobante.setText("");
        ejecutar(() -> {
            try (Connection c = ConexionOracle.obtener()) {
                PedidoDAOImpl dao = new PedidoDAOImpl(c); Pedido p = dao.buscarPorId(id);
                if (p == null) throw new IllegalArgumentException("No se encontró el pedido.");
                String texto = ("PAGADO".equals(p.getEstado()) || "ENTREGADO".equals(p.getEstado()))
                    ? new VentaServicio(c).comprobante(id).texto() : null;
                return new Consulta(p, dao.listarDetalle(id), texto);
            }
        }, consulta -> {
            pedido = consulta.pedido();
            if (consulta.comprobante() != null) comprobante.setText(consulta.comprobante());
            else {
                StringBuilder texto = new StringBuilder("Pedido #" + pedido.getIdPedido() + " | Cliente #" + pedido.getIdCliente()
                    + "\nEstado: " + pedido.getEstado() + "\nTotal: Q" + pedido.getTotal() + "\nPuntos previstos: " + pedido.getPuntosGenerados() + "\n\n");
                for (PedidoDetalle d : consulta.detalle()) texto.append(d.getTipoItem()).append(" #")
                    .append(d.getIdProducto() != null ? d.getIdProducto() : d.getIdMenu()).append(" | Cantidad: ").append(d.getCantidad())
                    .append(" | Q").append(d.getSubtotal()).append("\n");
                comprobante.setText(texto.toString());
            }
        }, "No se pudo consultar el pedido.");
    }
    private void cobrar() {
        if (pedido == null) return;
        MetodoPago seleccionado = (MetodoPago)metodo.getSelectedItem();
        BigDecimal importe = null;
        if (seleccionado == MetodoPago.EFECTIVO) {
            try { importe = new BigDecimal(recibido.getText().trim()); }
            catch (NumberFormatException e) { JOptionPane.showMessageDialog(this, "Introduce el efectivo recibido, usando punto decimal."); return; }
        }
        int id = pedido.getIdPedido(); BigDecimal efectivo = importe; boolean aprobada = tarjeta.getSelectedIndex() == 0;
        ejecutar(() -> {
            try (Connection c = ConexionOracle.obtener()) { return new VentaServicio(c).cobrar(id, seleccionado, efectivo, aprobada); }
        }, venta -> { pedido = venta.pedido(); comprobante.setText(venta.texto()); }, "No se pudo completar el cobro.");
    }
    private void entregar() {
        if (pedido == null) return; int id = pedido.getIdPedido();
        ejecutar(() -> {
            try (Connection c = ConexionOracle.obtener()) {
                VentaServicio servicio = new VentaServicio(c); servicio.entregar(id); return servicio.comprobante(id);
            }
        }, venta -> { pedido = venta.pedido(); comprobante.setText(venta.texto()); }, "No se pudo registrar la entrega.");
    }
    private void habilitarPago() {
        boolean pendiente = pedido != null && "PENDIENTE".equals(pedido.getEstado());
        recibido.setEnabled(pendiente && metodo.getSelectedItem() == MetodoPago.EFECTIVO);
        tarjeta.setEnabled(pendiente && metodo.getSelectedItem() == MetodoPago.TARJETA);
    }
    @Override protected void habilitarControles(boolean habilitar) {
        FormPedido.habilitar(getContentPane(), habilitar);
        cobrar.setEnabled(habilitar && pedido != null && "PENDIENTE".equals(pedido.getEstado()));
        entregar.setEnabled(habilitar && pedido != null && "PAGADO".equals(pedido.getEstado()));
        if (habilitar) habilitarPago();
    }
}

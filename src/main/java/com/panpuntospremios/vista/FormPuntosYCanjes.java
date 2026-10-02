package com.panpuntospremios.vista;

import com.panpuntospremios.dao.*;
import com.panpuntospremios.modelo.*;
import com.panpuntospremios.servicio.ClienteServicio;
import com.panpuntospremios.servicio.PuntosYCanjesServicio;
import com.panpuntospremios.util.ConexionOracle;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

public class FormPuntosYCanjes extends JFrame {

    // ── Pestaña 1: Canjear ───────────────────────────────────────────────────
    private final JTextField txtIdentificador = new JTextField(15);
    private final JButton btnBuscar = new JButton("Buscar cliente");
    private final JLabel lblSaldo = new JLabel("Saldo: —");
    private final DefaultTableModel modeloRecompensas = modelo("ID", "Recompensa", "Puntos necesarios");
    private final JTable tablaRecompensas = new JTable(modeloRecompensas);
    private final JButton btnCanjear = new JButton("Canjear seleccionada");

    // ── Pestaña 2: Administrar recompensas ───────────────────────────────────
    private final DefaultTableModel modeloAdmin = modelo("ID", "Nombre", "Puntos", "Producto ID", "Menú ID");
    private final JTable tablaAdmin = new JTable(modeloAdmin);
    private final JButton btnRegistrar = new JButton("Registrar");
    private final JButton btnEditar = new JButton("Editar");
    private final JButton btnDesactivar = new JButton("Desactivar");

    // ── Pestaña 3: Historial de puntos ───────────────────────────────────────
    private final DefaultTableModel modeloHistorial = modelo("Fecha", "Tipo", "Puntos", "Referencia");
    private final JTable tablaHistorial = new JTable(modeloHistorial);
    private final JButton btnVerHistorial = new JButton("Ver historial");

    // ── Pestaña 4: Historial de pedidos ──────────────────────────────────────
    private final DefaultTableModel modeloPedidos = modelo("ID", "Fecha", "Estado", "Total", "Puntos generados");
    private final JTable tablaPedidos = new JTable(modeloPedidos);
    private final JButton btnVerPedidos = new JButton("Ver pedidos");

    // ── Estado ───────────────────────────────────────────────────────────────
    private Connection conexion;
    private PuntosYCanjesServicio servicio;
    private ClienteServicio clienteServicio;
    private Cliente clienteActual;
    private volatile boolean cerrado;
    private SwingWorker<?, ?> tareaActual;

    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    /** Abre el formulario sin cliente pre-cargado. */
    public FormPuntosYCanjes() { this(null); }

    /** Abre el formulario con un cliente ya buscado (desde FormClientes). */
    public FormPuntosYCanjes(Cliente clienteInicial) {
        setTitle("Puntos y canjes");
        setSize(800, 560);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Canjear recompensa", panelCanjear());
        pestanas.addTab("Administrar recompensas", panelAdmin());
        pestanas.addTab("Historial de puntos", panelHistorial());
        pestanas.addTab("Historial de pedidos", panelPedidos());
        add(pestanas);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) { cerrarRecursos(); }
        });

        actualizarControles();
        conectar(clienteInicial);
    }

    // ── Construcción de paneles ──────────────────────────────────────────────

    private JPanel panelCanjear() {
        JPanel panelBuscar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelBuscar.add(new JLabel("Identificador:"));
        panelBuscar.add(txtIdentificador);
        panelBuscar.add(btnBuscar);
        panelBuscar.add(lblSaldo);

        tablaRecompensas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBoton.add(btnCanjear);

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        panel.add(panelBuscar, BorderLayout.NORTH);
        panel.add(new JScrollPane(tablaRecompensas), BorderLayout.CENTER);
        panel.add(panelBoton, BorderLayout.SOUTH);

        btnBuscar.addActionListener(e -> buscarCliente());
        btnCanjear.addActionListener(e -> canjear());
        return panel;
    }

    private JPanel panelAdmin() {
        tablaAdmin.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBotones.add(btnRegistrar);
        panelBotones.add(btnEditar);
        panelBotones.add(btnDesactivar);

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        panel.add(new JScrollPane(tablaAdmin), BorderLayout.CENTER);
        panel.add(panelBotones, BorderLayout.SOUTH);

        btnRegistrar.addActionListener(e -> registrarRecompensa());
        btnEditar.addActionListener(e -> editarRecompensa());
        btnDesactivar.addActionListener(e -> desactivarRecompensa());
        return panel;
    }

    private JPanel panelHistorial() {
        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelBoton.add(new JLabel("Cliente cargado en la pestaña Canjear recompensa."));
        panelBoton.add(btnVerHistorial);

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        panel.add(panelBoton, BorderLayout.NORTH);
        panel.add(new JScrollPane(tablaHistorial), BorderLayout.CENTER);

        btnVerHistorial.addActionListener(e -> cargarHistorial());
        return panel;
    }

    private JPanel panelPedidos() {
        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelBoton.add(new JLabel("Cliente cargado en la pestaña Canjear recompensa."));
        panelBoton.add(btnVerPedidos);

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        panel.add(panelBoton, BorderLayout.NORTH);
        panel.add(new JScrollPane(tablaPedidos), BorderLayout.CENTER);

        btnVerPedidos.addActionListener(e -> cargarPedidos());
        return panel;
    }

    // ── Conexión ─────────────────────────────────────────────────────────────

    private void conectar(Cliente clienteInicial) {
        new SwingWorker<Connection, Void>() {
            @Override protected Connection doInBackground() throws Exception {
                return ConexionOracle.obtener();
            }
            @Override protected void done() {
                try {
                    conexion = get();
                    servicio = new PuntosYCanjesServicio(conexion);
                    clienteServicio = new ClienteServicio(new ClienteDAOImpl(conexion));
                    if (clienteInicial != null) {
                        clienteActual = clienteInicial;
                        lblSaldo.setText("Saldo: " + clienteActual.getSaldoPuntos() +
                            " pts  |  " + clienteActual.getNombre());
                        txtIdentificador.setText(clienteActual.getIdentificador());
                    }
                    cargarRecompensas();
                } catch (CancellationException ignored) {
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    mostrarError("No se pudo conectar.", e.getCause());
                }
            }
        }.execute();
    }

    // ── Operaciones: pestaña Canjear ─────────────────────────────────────────

    private void cargarRecompensas() {
        ejecutar(() -> servicio.listarRecompensasActivas(), lista -> {
            modeloRecompensas.setRowCount(0);
            for (Recompensa r : lista)
                modeloRecompensas.addRow(new Object[]{r.getIdRecompensa(), r.getNombre(), r.getPuntosNecesarios()});
            cargarAdmin(lista);
        }, "Error al cargar recompensas.");
    }

    private void buscarCliente() {
        String id = txtIdentificador.getText().trim();
        if (id.isBlank()) { JOptionPane.showMessageDialog(this, "Ingresa un identificador."); return; }
        ejecutar(() -> clienteServicio.buscarPorIdentificador(id), cliente -> {
            if (cliente == null) {
                JOptionPane.showMessageDialog(this, "No se encontró el cliente.", "Sin resultados", JOptionPane.INFORMATION_MESSAGE);
                clienteActual = null;
                lblSaldo.setText("Saldo: —");
                modeloHistorial.setRowCount(0);
            } else {
                clienteActual = cliente;
                lblSaldo.setText("Saldo: " + cliente.getSaldoPuntos() + " pts  |  " + cliente.getNombre());
                cargarHistorial();
                cargarPedidos();
            }
            actualizarControles();
        }, "Error al buscar el cliente.");
    }

    private void canjear() {
        if (clienteActual == null) { JOptionPane.showMessageDialog(this, "Busca un cliente primero."); return; }
        int fila = tablaRecompensas.getSelectedRow();
        if (fila < 0) { JOptionPane.showMessageDialog(this, "Selecciona una recompensa."); return; }

        int idRecompensa = (int) modeloRecompensas.getValueAt(fila, 0);
        String nombre = (String) modeloRecompensas.getValueAt(fila, 1);
        int puntos = (int) modeloRecompensas.getValueAt(fila, 2);

        if (JOptionPane.showConfirmDialog(this,
                "¿Canjear \"" + nombre + "\" por " + puntos + " puntos para " + clienteActual.getNombre() + "?",
                "Confirmar canje", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        int idCliente = clienteActual.getIdCliente();
        String identificador = clienteActual.getIdentificador();
        ejecutar(() -> {
            servicio.canjear(idCliente, idRecompensa);
            return clienteServicio.buscarPorIdentificador(identificador);
        }, actualizado -> {
            clienteActual = actualizado;
            lblSaldo.setText("Saldo: " + clienteActual.getSaldoPuntos() + " pts  |  " + clienteActual.getNombre());
            cargarHistorial();
            JOptionPane.showMessageDialog(this, "Canje realizado correctamente.");
        }, "Error al realizar el canje.");
    }

    // ── Operaciones: pestaña Administrar recompensas ─────────────────────────

    private void cargarAdmin(List<Recompensa> lista) {
        modeloAdmin.setRowCount(0);
        for (Recompensa r : lista)
            modeloAdmin.addRow(new Object[]{
                r.getIdRecompensa(), r.getNombre(), r.getPuntosNecesarios(),
                r.getIdProducto() != null ? r.getIdProducto() : "",
                r.getIdMenu() != null ? r.getIdMenu() : ""});
    }

    private void registrarRecompensa() {
        JTextField txtNombre = new JTextField();
        JTextField txtPuntos = new JTextField();
        JTextField txtIdProducto = new JTextField();
        JTextField txtIdMenu = new JTextField();
        Object[] campos = {
            "Nombre:", txtNombre,
            "Puntos necesarios:", txtPuntos,
            "ID producto (dejar vacío si es menú):", txtIdProducto,
            "ID menú (dejar vacío si es producto):", txtIdMenu
        };
        if (JOptionPane.showConfirmDialog(this, campos, "Registrar recompensa", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) return;

        Recompensa r;
        try {
            r = new Recompensa(0, txtNombre.getText().trim(), entero(txtPuntos.getText(), "puntos"),
                parseNullable(txtIdProducto.getText()), parseNullable(txtIdMenu.getText()), true);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ejecutar(() -> { servicio.registrarRecompensa(r); return servicio.listarRecompensasActivas(); },
            lista -> { cargarAdmin(lista); JOptionPane.showMessageDialog(this, "Recompensa registrada."); },
            "Error al registrar la recompensa.");
    }

    private void editarRecompensa() {
        int fila = tablaAdmin.getSelectedRow();
        if (fila < 0) { JOptionPane.showMessageDialog(this, "Selecciona una recompensa."); return; }

        int id = (int) modeloAdmin.getValueAt(fila, 0);
        JTextField txtNombre = new JTextField((String) modeloAdmin.getValueAt(fila, 1));
        JTextField txtPuntos = new JTextField(modeloAdmin.getValueAt(fila, 2).toString());
        Object[] campos = {"Nombre:", txtNombre, "Puntos necesarios:", txtPuntos};
        if (JOptionPane.showConfirmDialog(this, campos, "Editar recompensa", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) return;

        Object rawProd = modeloAdmin.getValueAt(fila, 3);
        Object rawMenu = modeloAdmin.getValueAt(fila, 4);
        Recompensa r;
        try {
            r = new Recompensa(id, txtNombre.getText().trim(), entero(txtPuntos.getText(), "puntos"),
                rawProd instanceof Integer ? (Integer) rawProd : null,
                rawMenu instanceof Integer ? (Integer) rawMenu : null, true);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ejecutar(() -> { servicio.actualizarRecompensa(r); return servicio.listarRecompensasActivas(); },
            lista -> { cargarAdmin(lista); modeloRecompensas.setRowCount(0);
                lista.forEach(rec -> modeloRecompensas.addRow(new Object[]{rec.getIdRecompensa(), rec.getNombre(), rec.getPuntosNecesarios()}));
                JOptionPane.showMessageDialog(this, "Recompensa actualizada."); },
            "Error al actualizar la recompensa.");
    }

    private void desactivarRecompensa() {
        int fila = tablaAdmin.getSelectedRow();
        if (fila < 0) { JOptionPane.showMessageDialog(this, "Selecciona una recompensa."); return; }
        int id = (int) modeloAdmin.getValueAt(fila, 0);
        String nombre = (String) modeloAdmin.getValueAt(fila, 1);
        if (JOptionPane.showConfirmDialog(this, "¿Desactivar \"" + nombre + "\"?",
                "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        ejecutar(() -> { servicio.desactivarRecompensa(id); return servicio.listarRecompensasActivas(); },
            lista -> { cargarAdmin(lista); modeloRecompensas.setRowCount(0);
                lista.forEach(r -> modeloRecompensas.addRow(new Object[]{r.getIdRecompensa(), r.getNombre(), r.getPuntosNecesarios()}));
                JOptionPane.showMessageDialog(this, "Recompensa desactivada."); },
            "Error al desactivar la recompensa.");
    }

    // ── Operaciones: pestaña Historial de puntos ─────────────────────────────

    private void cargarHistorial() {
        if (clienteActual == null) return;
        int id = clienteActual.getIdCliente();
        ejecutar(() -> servicio.listarMovimientosPuntos(id), movimientos -> {
            modeloHistorial.setRowCount(0);
            for (MovimientoPuntos m : movimientos)
                modeloHistorial.addRow(new Object[]{
                    SDF.format(m.getFecha()), m.getTipo(), m.getPuntos(), m.getReferencia()});
        }, "Error al cargar el historial de puntos.");
    }

    private void cargarPedidos() {
        if (clienteActual == null) return;
        int id = clienteActual.getIdCliente();
        ejecutar(() -> servicio.listarPedidosPorCliente(id), lista -> {
            modeloPedidos.setRowCount(0);
            for (com.panpuntospremios.modelo.Pedido p : lista)
                modeloPedidos.addRow(new Object[]{
                    p.getIdPedido(), SDF.format(p.getFecha()),
                    p.getEstado(), p.getTotal(), p.getPuntosGenerados()});
        }, "Error al cargar el historial de pedidos.");
    }

    // ── Infraestructura ──────────────────────────────────────────────────────

    private <T> void ejecutar(java.util.concurrent.Callable<T> op,
                               java.util.function.Consumer<T> ok, String msgError) {
        actualizarControles();
        tareaActual = new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return op.call(); }
            @Override protected void done() {
                try {
                    if (!cerrado) ok.accept(get());
                } catch (CancellationException ignored) {
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); mostrarError(msgError, e);
                } catch (ExecutionException e) {
                    mostrarError(msgError, e.getCause());
                } finally { tareaActual = null; actualizarControles(); }
            }
        };
        tareaActual.execute();
    }

    private void actualizarControles() {
        boolean listo = conexion != null && !cerrado && tareaActual == null;
        boolean conCliente = listo && clienteActual != null;
        btnBuscar.setEnabled(listo);
        txtIdentificador.setEnabled(listo);
        btnCanjear.setEnabled(conCliente);
        btnRegistrar.setEnabled(listo);
        btnEditar.setEnabled(listo);
        btnDesactivar.setEnabled(listo);
        btnVerHistorial.setEnabled(conCliente);
        btnVerPedidos.setEnabled(conCliente);
        setCursor(Cursor.getPredefinedCursor(
            tareaActual != null ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void mostrarError(String ctx, Throwable e) {
        if (cerrado) return;
        String msg = e != null && e.getMessage() != null ? e.getMessage() : "Consulta el registro.";
        JOptionPane.showMessageDialog(this, ctx + "\n" + msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void cerrarRecursos() {
        cerrado = true;
        if (tareaActual != null) tareaActual.cancel(true);
        if (conexion != null) try { conexion.close(); } catch (Exception ignored) {}
    }

    private static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private static int entero(String texto, String campo) {
        try { return Integer.parseInt(texto.trim()); }
        catch (NumberFormatException e) {
            throw new com.panpuntospremios.excepcion.ValidacionException("El campo " + campo + " debe ser un número entero.");
        }
    }

    private static Integer parseNullable(String texto) {
        String t = texto.trim();
        if (t.isBlank()) return null;
        try { return Integer.parseInt(t); }
        catch (NumberFormatException e) {
            throw new com.panpuntospremios.excepcion.ValidacionException("El ID debe ser un número entero o dejarse vacío.");
        }
    }
}

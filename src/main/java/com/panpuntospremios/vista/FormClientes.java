package com.panpuntospremios.vista;

import com.panpuntospremios.dao.ClienteDAOImpl;
import com.panpuntospremios.modelo.Cliente;
import com.panpuntospremios.servicio.ClienteServicio;
import com.panpuntospremios.util.ConexionOracle;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class FormClientes extends JFrame {

    private final DefaultTableModel modeloTabla;
    private final JTable tabla;
    private final JTextField txtBuscar;
    private final JButton btnBuscar;
    private final JButton btnMostrarTodos;
    private final JButton btnRegistrar;
    private final JButton btnEditar;
    private final JButton btnDesactivar;
    private final JButton btnVerPuntos;

    private ClienteServicio servicio;
    private volatile Connection conexion;
    private SwingWorker<?, ?> tareaActual;
    private volatile boolean cerrado;

    public FormClientes() {
        setTitle("Gestión de Clientes");
        setSize(900, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panelBuscar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        txtBuscar = new JTextField(20);
        btnBuscar = new JButton("Buscar");
        btnMostrarTodos = new JButton("Mostrar todos");
        panelBuscar.add(new JLabel("Identificador:"));
        panelBuscar.add(txtBuscar);
        panelBuscar.add(btnBuscar);
        panelBuscar.add(btnMostrarTodos);

        modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Identificador", "Nombre", "Teléfono", "Email", "Puntos"},
            0
        ) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tabla = new JTable(modeloTabla);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnVerPuntos = new JButton("Ver puntos e historial");
        btnRegistrar = new JButton("Registrar");
        btnEditar = new JButton("Editar");
        btnDesactivar = new JButton("Desactivar");
        panelBotones.add(btnVerPuntos);
        panelBotones.add(btnRegistrar);
        panelBotones.add(btnEditar);
        panelBotones.add(btnDesactivar);

        setLayout(new BorderLayout());
        add(panelBuscar, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(panelBotones, BorderLayout.SOUTH);

        btnBuscar.addActionListener(e -> buscar());
        btnMostrarTodos.addActionListener(e -> cargarTodos());
        btnRegistrar.addActionListener(e -> registrar());
        btnEditar.addActionListener(e -> editar());
        btnDesactivar.addActionListener(e -> desactivar());
        btnVerPuntos.addActionListener(e -> verPuntos());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) { cerrarRecursos(); }
        });

        actualizarEstadoControles(false);
        conectarYCargar();
    }

    private void verPuntos() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un cliente de la tabla.");
            return;
        }
        Cliente cliente = new Cliente(
            ((Number) modeloTabla.getValueAt(fila, 0)).intValue(),
            (String) modeloTabla.getValueAt(fila, 1),
            (String) modeloTabla.getValueAt(fila, 2),
            (String) modeloTabla.getValueAt(fila, 3),
            (String) modeloTabla.getValueAt(fila, 4),
            ((Number) modeloTabla.getValueAt(fila, 5)).intValue(),
            true, null
        );
        new FormPuntosYCanjes(cliente).setVisible(true);
    }

    private void conectarYCargar() {
        ejecutarEnSegundoPlano(() -> {
            Connection nuevaConexion = ConexionOracle.obtener();
            conexion = nuevaConexion;
            try {
                if (cerrado) throw new CancellationException();
                ClienteServicio nuevoServicio = new ClienteServicio(new ClienteDAOImpl(nuevaConexion));
                List<Cliente> clientes = nuevoServicio.listarActivos();
                return new ResultadoConexion(nuevaConexion, nuevoServicio, clientes);
            } catch (Exception e) {
                try { nuevaConexion.close(); } catch (Exception errorCierre) { e.addSuppressed(errorCierre); }
                conexion = null;
                throw e;
            }
        }, resultado -> {
            conexion = resultado.conexion();
            servicio = resultado.servicio();
            mostrarClientes(resultado.clientes());
        }, "No se pudo conectar y cargar los clientes.");
    }

    private void cargarTodos() {
        ejecutarEnSegundoPlano(() -> servicio.listarActivos(), this::mostrarClientes, "Error al cargar clientes.");
    }

    private void buscar() {
        String identificador = txtBuscar.getText().trim();
        if (identificador.isBlank()) { cargarTodos(); return; }
        ejecutarEnSegundoPlano(
            () -> servicio.buscarPorIdentificador(identificador),
            cliente -> {
                modeloTabla.setRowCount(0);
                if (cliente == null) {
                    JOptionPane.showMessageDialog(this,
                        "No se encontró ningún cliente con ese identificador.",
                        "Sin resultados", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                agregarFila(cliente);
            },
            "Error al buscar el cliente."
        );
    }

    private void registrar() {
        JTextField txtId = new JTextField();
        JTextField txtNombre = new JTextField();
        JTextField txtTelefono = new JTextField();
        JTextField txtEmail = new JTextField();
        configurarCamposCliente(txtId, txtNombre, txtTelefono, txtEmail);

        Object[] campos = {"Identificador:", txtId, "Nombre:", txtNombre, "Teléfono:", txtTelefono, "Email:", txtEmail};
        if (JOptionPane.showConfirmDialog(this, campos, "Registrar cliente", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) return;

        Cliente cliente = new Cliente(0, txtId.getText().trim(), txtNombre.getText().trim(),
            txtTelefono.getText().trim(), txtEmail.getText().trim(), 0, true, null);
        ejecutarEnSegundoPlano(() -> { servicio.registrar(cliente); return servicio.listarActivos(); },
            clientes -> { mostrarClientes(clientes); JOptionPane.showMessageDialog(this, "Cliente registrado correctamente."); },
            "Error al registrar el cliente.");
    }

    private void editar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) { JOptionPane.showMessageDialog(this, "Selecciona un cliente de la tabla."); return; }

        int idCliente = ((Number) modeloTabla.getValueAt(fila, 0)).intValue();
        JTextField txtNombre = new JTextField((String) modeloTabla.getValueAt(fila, 2));
        JTextField txtTelefono = new JTextField((String) modeloTabla.getValueAt(fila, 3));
        JTextField txtEmail = new JTextField((String) modeloTabla.getValueAt(fila, 4));
        configurarCamposCliente(null, txtNombre, txtTelefono, txtEmail);

        Object[] campos = {"Nombre:", txtNombre, "Teléfono:", txtTelefono, "Email:", txtEmail};
        if (JOptionPane.showConfirmDialog(this, campos, "Editar cliente", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) return;

        Cliente cliente = new Cliente(idCliente, (String) modeloTabla.getValueAt(fila, 1),
            txtNombre.getText().trim(), txtTelefono.getText().trim(), txtEmail.getText().trim(), 0, true, null);
        ejecutarEnSegundoPlano(() -> { servicio.actualizar(cliente); return servicio.listarActivos(); },
            clientes -> { mostrarClientes(clientes); JOptionPane.showMessageDialog(this, "Cliente actualizado correctamente."); },
            "Error al actualizar el cliente.");
    }

    private void desactivar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) { JOptionPane.showMessageDialog(this, "Selecciona un cliente de la tabla."); return; }

        int idCliente = ((Number) modeloTabla.getValueAt(fila, 0)).intValue();
        String nombre = (String) modeloTabla.getValueAt(fila, 2);
        if (JOptionPane.showConfirmDialog(this, "¿Desactivar al cliente " + nombre + "?",
                "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        ejecutarEnSegundoPlano(() -> { servicio.desactivar(idCliente); return servicio.listarActivos(); },
            clientes -> { mostrarClientes(clientes); JOptionPane.showMessageDialog(this, "Cliente desactivado correctamente."); },
            "Error al desactivar el cliente.");
    }

    private void mostrarClientes(List<Cliente> clientes) {
        modeloTabla.setRowCount(0);
        clientes.forEach(this::agregarFila);
    }

    private void agregarFila(Cliente cliente) {
        modeloTabla.addRow(new Object[]{
            cliente.getIdCliente(), cliente.getIdentificador(), cliente.getNombre(),
            cliente.getTelefono(), cliente.getEmail(), cliente.getSaldoPuntos()
        });
    }

    private <T> void ejecutarEnSegundoPlano(Callable<T> operacion, Consumer<T> alCompletar, String mensajeError) {
        actualizarEstadoControles(true);
        tareaActual = new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return operacion.call(); }
            @Override protected void done() {
                try {
                    if (!cerrado) alCompletar.accept(get());
                } catch (CancellationException ignored) {
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); mostrarError(mensajeError, e);
                } catch (ExecutionException e) {
                    mostrarError(mensajeError, e.getCause());
                } finally { tareaActual = null; actualizarEstadoControles(false); }
            }
        };
        tareaActual.execute();
    }

    private void actualizarEstadoControles(boolean operando) {
        boolean habilitar = !operando && servicio != null && !cerrado;
        txtBuscar.setEnabled(habilitar);
        tabla.setEnabled(habilitar);
        btnBuscar.setEnabled(habilitar);
        btnMostrarTodos.setEnabled(habilitar);
        btnRegistrar.setEnabled(habilitar);
        btnEditar.setEnabled(habilitar);
        btnDesactivar.setEnabled(habilitar);
        btnVerPuntos.setEnabled(habilitar);
        setCursor(Cursor.getPredefinedCursor(operando ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void mostrarError(String contexto, Throwable error) {
        if (cerrado) return;
        String detalle = error == null || error.getMessage() == null || error.getMessage().isBlank()
            ? "Consulta el registro de la aplicación." : error.getMessage();
        JOptionPane.showMessageDialog(this, contexto + System.lineSeparator() + detalle, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void cerrarRecursos() {
        cerrado = true;
        if (tareaActual != null) tareaActual.cancel(true);
        if (conexion != null) try { conexion.close(); } catch (Exception ignored) {}
    }

    private void configurarCamposCliente(JTextField identificador, JTextField nombre, JTextField telefono, JTextField email) {
        if (identificador != null) {
            aplicarFiltro(identificador, Pattern.compile("(?:[\\p{L}\\p{N}][\\p{L}\\p{N}_-]*)?"), 20);
            identificador.setToolTipText("Letras y números; puede incluir _ o - después del primer carácter.");
        }
        aplicarFiltro(nombre, Pattern.compile("[\\p{L} '\\-]*"), 100);
        aplicarFiltro(telefono, Pattern.compile("\\+?[0-9() .-]*"), 20);
        aplicarFiltro(email, Pattern.compile("(?s).*"), 100);
        nombre.setToolTipText("Solo letras, espacios, apóstrofes y guiones.");
        telefono.setToolTipText("Entre 7 y 15 dígitos; admite +, espacios, paréntesis y guiones.");
        email.setToolTipText("Ejemplo: nombre@dominio.com");
    }

    private void aplicarFiltro(JTextField campo, Pattern patronPermitido, int longitudMaxima) {
        ((AbstractDocument) campo.getDocument()).setDocumentFilter(new FiltroCampo(patronPermitido, longitudMaxima));
    }

    private static class FiltroCampo extends DocumentFilter {
        private final Pattern patronPermitido;
        private final int longitudMaxima;

        private FiltroCampo(Pattern patronPermitido, int longitudMaxima) {
            this.patronPermitido = patronPermitido;
            this.longitudMaxima = longitudMaxima;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String texto, AttributeSet atributos)
                throws BadLocationException {
            replace(fb, offset, 0, texto, atributos);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int longitud, String texto, AttributeSet atributos)
                throws BadLocationException {
            String actual = fb.getDocument().getText(0, fb.getDocument().getLength());
            String nuevo = new StringBuilder(actual)
                .replace(offset, offset + longitud, texto == null ? "" : texto).toString();
            if (nuevo.length() <= longitudMaxima && patronPermitido.matcher(nuevo).matches())
                fb.replace(offset, longitud, texto, atributos);
        }
    }

    private record ResultadoConexion(Connection conexion, ClienteServicio servicio, List<Cliente> clientes) {}
}

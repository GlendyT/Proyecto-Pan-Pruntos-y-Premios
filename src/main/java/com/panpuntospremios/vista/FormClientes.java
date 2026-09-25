package com.panpuntospremios.vista;

import com.panpuntospremios.dao.ClienteDAOImpl;
import com.panpuntospremios.modelo.Cliente;
import com.panpuntospremios.servicio.ClienteServicio;
import com.panpuntospremios.util.ConexionOracle;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.util.List;

public class FormClientes extends JFrame {

    private final DefaultTableModel modeloTabla;
    private final JTable tabla;
    private final JTextField txtBuscar;
    private ClienteServicio servicio;
    private Connection conexion;

    public FormClientes() {
        setTitle("Gestión de Clientes");
        setSize(800, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        try {
            conexion = ConexionOracle.obtener();
            servicio = new ClienteServicio(new ClienteDAOImpl(conexion));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al conectar con la base de datos.", "Error", JOptionPane.ERROR_MESSAGE);
        }

        // Panel de búsqueda
        JPanel panelBuscar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        txtBuscar = new JTextField(20);
        JButton btnBuscar = new JButton("Buscar");
        JButton btnMostrarTodos = new JButton("Mostrar todos");
        panelBuscar.add(new JLabel("Identificador:"));
        panelBuscar.add(txtBuscar);
        panelBuscar.add(btnBuscar);
        panelBuscar.add(btnMostrarTodos);

        // Tabla
        modeloTabla = new DefaultTableModel(new Object[]{"ID", "Identificador", "Nombre", "Teléfono", "Email", "Puntos"}, 0) {
            public boolean isCellEditable(int row, int col) { return false; }
        };
        tabla = new JTable(modeloTabla);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Panel de botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnRegistrar = new JButton("Registrar");
        JButton btnEditar = new JButton("Editar");
        JButton btnDesactivar = new JButton("Desactivar");
        panelBotones.add(btnRegistrar);
        panelBotones.add(btnEditar);
        panelBotones.add(btnDesactivar);

        // Layout
        setLayout(new BorderLayout());
        add(panelBuscar, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(panelBotones, BorderLayout.SOUTH);

        // Acciones
        btnBuscar.addActionListener(e -> buscar());
        btnMostrarTodos.addActionListener(e -> cargarTodos());
        btnRegistrar.addActionListener(e -> registrar());
        btnEditar.addActionListener(e -> editar());
        btnDesactivar.addActionListener(e -> desactivar());

        cargarTodos();
    }

    private void cargarTodos() {
        try {
            List<Cliente> clientes = servicio.listarActivos();
            modeloTabla.setRowCount(0);
            for (Cliente c : clientes) {
                modeloTabla.addRow(new Object[]{c.getIdCliente(), c.getIdentificador(), c.getNombre(), c.getTelefono(), c.getEmail(), c.getSaldoPuntos()});
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al cargar clientes: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void buscar() {
        String identificador = txtBuscar.getText().trim();
        if (identificador.isBlank()) { cargarTodos(); return; }
        try {
            Cliente c = servicio.buscarPorIdentificador(identificador);
            modeloTabla.setRowCount(0);
            if (c != null) {
                modeloTabla.addRow(new Object[]{c.getIdCliente(), c.getIdentificador(), c.getNombre(), c.getTelefono(), c.getEmail(), c.getSaldoPuntos()});
            } else {
                JOptionPane.showMessageDialog(this, "No se encontró ningún cliente con ese identificador.", "Sin resultados", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al buscar: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void registrar() {
        JTextField txtId = new JTextField();
        JTextField txtNombre = new JTextField();
        JTextField txtTelefono = new JTextField();
        JTextField txtEmail = new JTextField();

        Object[] campos = {"Identificador:", txtId, "Nombre:", txtNombre, "Teléfono:", txtTelefono, "Email:", txtEmail};
        int resultado = JOptionPane.showConfirmDialog(this, campos, "Registrar cliente", JOptionPane.OK_CANCEL_OPTION);
        if (resultado != JOptionPane.OK_OPTION) return;

        try {
            Cliente cliente = new Cliente(0, txtId.getText().trim(), txtNombre.getText().trim(), txtTelefono.getText().trim(), txtEmail.getText().trim(), 0, true, null);
            servicio.registrar(cliente);
            JOptionPane.showMessageDialog(this, "Cliente registrado correctamente.");
            cargarTodos();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) { JOptionPane.showMessageDialog(this, "Selecciona un cliente de la tabla."); return; }

        int idCliente = (int) modeloTabla.getValueAt(fila, 0);
        JTextField txtNombre = new JTextField((String) modeloTabla.getValueAt(fila, 2));
        JTextField txtTelefono = new JTextField((String) modeloTabla.getValueAt(fila, 3));
        JTextField txtEmail = new JTextField((String) modeloTabla.getValueAt(fila, 4));

        Object[] campos = {"Nombre:", txtNombre, "Teléfono:", txtTelefono, "Email:", txtEmail};
        int resultado = JOptionPane.showConfirmDialog(this, campos, "Editar cliente", JOptionPane.OK_CANCEL_OPTION);
        if (resultado != JOptionPane.OK_OPTION) return;

        try {
            Cliente cliente = new Cliente(idCliente, (String) modeloTabla.getValueAt(fila, 1), txtNombre.getText().trim(), txtTelefono.getText().trim(), txtEmail.getText().trim(), 0, true, null);
            servicio.actualizar(cliente);
            JOptionPane.showMessageDialog(this, "Cliente actualizado correctamente.");
            cargarTodos();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void desactivar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) { JOptionPane.showMessageDialog(this, "Selecciona un cliente de la tabla."); return; }

        int idCliente = (int) modeloTabla.getValueAt(fila, 0);
        String nombre = (String) modeloTabla.getValueAt(fila, 2);
        int confirmacion = JOptionPane.showConfirmDialog(this, "¿Desactivar al cliente " + nombre + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirmacion != JOptionPane.YES_OPTION) return;

        try {
            servicio.desactivar(idCliente);
            JOptionPane.showMessageDialog(this, "Cliente desactivado correctamente.");
            cargarTodos();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

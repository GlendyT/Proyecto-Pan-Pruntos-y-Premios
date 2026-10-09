package com.panpuntospremios.vista;

import com.panpuntospremios.dao.ReporteDAOImpl;
import com.panpuntospremios.modelo.ReporteDatos;
import com.panpuntospremios.servicio.ReporteServicio;
import com.panpuntospremios.util.ConexionOracle;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class FormReportes extends FormularioAsincrono {
    private final JComboBox<ReporteServicio.Tipo> reporte = new JComboBox<>(ReporteServicio.Tipo.values());
    private final JTextField desde = new JTextField(LocalDate.now().minusDays(30).toString(), 10);
    private final JTextField hasta = new JTextField(LocalDate.now().toString(), 10);
    private final JLabel etiquetaFiltro = new JLabel("Estado:");
    private final JComboBox<String> filtro = new JComboBox<>();
    private final DefaultTableModel modelo = new DefaultTableModel() {
        @Override public boolean isCellEditable(int fila, int columna) { return false; }
    };
    private final JTable tabla = new JTable(modelo);

    public FormReportes() {
        setTitle("Reportes"); setSize(1050, 650); setLocationRelativeTo(null);
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filtros.add(new JLabel("Reporte:")); filtros.add(reporte);
        filtros.add(new JLabel("Desde (AAAA-MM-DD):")); filtros.add(desde);
        filtros.add(new JLabel("Hasta (AAAA-MM-DD):")); filtros.add(hasta);
        filtros.add(etiquetaFiltro); filtros.add(filtro);
        JButton consultar = new JButton("Consultar"); filtros.add(consultar);
        add(filtros, BorderLayout.NORTH);
        tabla.setAutoCreateRowSorter(true);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        reporte.addActionListener(e -> actualizarFiltros());
        consultar.addActionListener(e -> consultar());
        actualizarFiltros();
    }

    private void actualizarFiltros() {
        ReporteServicio.Tipo tipo = (ReporteServicio.Tipo)reporte.getSelectedItem();
        if (tipo == null) return;
        String[] opciones = switch (tipo) {
            case VENTAS -> new String[]{"TODOS", "PAGADO", "ENTREGADO"};
            case INVENTARIO -> new String[]{"TODOS", "ACTIVOS", "INACTIVOS"};
            case MOVIMIENTOS_INVENTARIO -> new String[]{"TODOS", "ABASTECIMIENTO", "VENTA", "CANJE"};
            case MOVIMIENTOS_PUNTOS -> new String[]{"TODOS", "ACUMULACION", "CANJE"};
            default -> new String[]{"TODOS"};
        };
        filtro.setModel(new DefaultComboBoxModel<>(opciones));
        etiquetaFiltro.setText(tipo == ReporteServicio.Tipo.VENTAS ? "Estado:" :
            tipo == ReporteServicio.Tipo.INVENTARIO ? "Activo:" : "Tipo:");
        boolean rango = tipo != ReporteServicio.Tipo.INVENTARIO;
        desde.setEnabled(rango); hasta.setEnabled(rango);
    }

    private void consultar() {
        final LocalDate fechaDesde, fechaHasta;
        try {
            fechaDesde = desde.getText().isBlank() ? null : LocalDate.parse(desde.getText().trim());
            fechaHasta = hasta.getText().isBlank() ? null : LocalDate.parse(hasta.getText().trim());
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Usa fechas válidas en formato AAAA-MM-DD.", "Filtro inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ReporteServicio.Tipo tipo = (ReporteServicio.Tipo)reporte.getSelectedItem();
        String criterio = (String)filtro.getSelectedItem();
        ejecutar(() -> {
            try (Connection conexion = ConexionOracle.obtener()) {
                return new ReporteServicio(new ReporteDAOImpl(conexion))
                    .consultar(tipo, fechaDesde, fechaHasta, criterio);
            }
        }, this::mostrar, "No se pudo generar el reporte.");
    }

    private void mostrar(ReporteDatos resultado) {
        modelo.setRowCount(0);
        modelo.setColumnIdentifiers(resultado.columnas().toArray());
        for (var fila : resultado.filas()) modelo.addRow(fila.toArray());
        setTitle("Reportes — " + resultado.titulo() + " (" + resultado.filas().size() + ")");
    }

    @Override protected void habilitarControles(boolean habilitar) {
        FormPedido.habilitar(getContentPane(), habilitar);
        ReporteServicio.Tipo tipo = (ReporteServicio.Tipo)reporte.getSelectedItem();
        boolean rango = habilitar && tipo != ReporteServicio.Tipo.INVENTARIO;
        desde.setEnabled(rango); hasta.setEnabled(rango);
    }
}

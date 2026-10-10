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
    private static final ReporteServicio.Tipo[] TIPOS_ORDENADOS = {
        ReporteServicio.Tipo.VENTAS_POR_PERIODO,
        ReporteServicio.Tipo.PRODUCTOS_MAS_VENDIDOS,
        ReporteServicio.Tipo.BAJO_INVENTARIO,
        ReporteServicio.Tipo.PUNTOS_POR_CLIENTE,
        ReporteServicio.Tipo.HISTORIAL_CANJES,
        ReporteServicio.Tipo.VENTAS_MENUS,
        ReporteServicio.Tipo.MOVIMIENTOS_INVENTARIO,
        ReporteServicio.Tipo.MOVIMIENTOS_PUNTOS,
        ReporteServicio.Tipo.INVENTARIO,
        ReporteServicio.Tipo.VENTAS
    };

    private final JComboBox<ReporteServicio.Tipo> reporte = new JComboBox<>(TIPOS_ORDENADOS);
    private final JTextField desde = new JTextField(LocalDate.now().minusDays(30).toString(), 10);
    private final JTextField hasta = new JTextField(LocalDate.now().toString(), 10);
    private final JLabel etiquetaFiltro = new JLabel("Estado:");
    private final JComboBox<String> filtro = new JComboBox<>();
    private final DefaultTableModel modelo = new DefaultTableModel() {
        @Override public boolean isCellEditable(int fila, int columna) { return false; }
    };
    private final JTable tabla = new JTable(modelo);

    public FormReportes() {
        setTitle("Reportes"); setSize(1100, 650); setLocationRelativeTo(null);

        reporte.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ReporteServicio.Tipo t) {
                    setText(switch (t) {
                        case VENTAS_POR_PERIODO -> "01. Ventas por período";
                        case PRODUCTOS_MAS_VENDIDOS -> "02. Productos más vendidos";
                        case BAJO_INVENTARIO -> "03. Productos agotados o bajo inventario";
                        case PUNTOS_POR_CLIENTE -> "04. Puntos acumulados por cliente";
                        case HISTORIAL_CANJES -> "05. Historial de canjes";
                        case VENTAS_MENUS -> "06. Ventas de menús completos";
                        case MOVIMIENTOS_INVENTARIO -> "Movimientos de inventario (general)";
                        case MOVIMIENTOS_PUNTOS -> "Movimientos de puntos (general)";
                        case INVENTARIO -> "Inventario general de productos";
                        case VENTAS -> "Ventas generales (histórico)";
                    });
                }
                return this;
            }
        });

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
        ReporteServicio.Tipo tipo = (ReporteServicio.Tipo) reporte.getSelectedItem();
        if (tipo == null) return;
        String[] opciones = switch (tipo) {
            case VENTAS_POR_PERIODO -> new String[]{"TODOS", "EFECTIVO", "TARJETA"};
            case VENTAS -> new String[]{"TODOS", "PAGADO", "ENTREGADO"};
            case INVENTARIO -> new String[]{"TODOS", "ACTIVOS", "INACTIVOS"};
            case MOVIMIENTOS_INVENTARIO -> new String[]{"TODOS", "ABASTECIMIENTO", "VENTA", "CANJE"};
            case MOVIMIENTOS_PUNTOS -> new String[]{"TODOS", "ACUMULACION", "CANJE"};
            default -> new String[]{"TODOS"};
        };
        filtro.setModel(new DefaultComboBoxModel<>(opciones));
        etiquetaFiltro.setText(switch (tipo) {
            case VENTAS_POR_PERIODO -> "Método pago:";
            case VENTAS -> "Estado:";
            case INVENTARIO -> "Activo:";
            default -> "Filtro:";
        });
        boolean requiereRango = (tipo == ReporteServicio.Tipo.VENTAS_POR_PERIODO
            || tipo == ReporteServicio.Tipo.VENTAS
            || tipo == ReporteServicio.Tipo.MOVIMIENTOS_INVENTARIO
            || tipo == ReporteServicio.Tipo.MOVIMIENTOS_PUNTOS);
        desde.setEnabled(requiereRango);
        hasta.setEnabled(requiereRango);
        filtro.setEnabled(opciones.length > 1);
    }

    private void consultar() {
        ReporteServicio.Tipo tipo = (ReporteServicio.Tipo) reporte.getSelectedItem();
        if (tipo == null) return;

        boolean requiereRango = (tipo == ReporteServicio.Tipo.VENTAS_POR_PERIODO
            || tipo == ReporteServicio.Tipo.VENTAS
            || tipo == ReporteServicio.Tipo.MOVIMIENTOS_INVENTARIO
            || tipo == ReporteServicio.Tipo.MOVIMIENTOS_PUNTOS);

        final LocalDate fechaDesde, fechaHasta;
        if (requiereRango) {
            try {
                fechaDesde = desde.getText().isBlank() ? null : LocalDate.parse(desde.getText().trim());
                fechaHasta = hasta.getText().isBlank() ? null : LocalDate.parse(hasta.getText().trim());
            } catch (DateTimeParseException e) {
                JOptionPane.showMessageDialog(this, "Usa fechas válidas en formato AAAA-MM-DD.", "Filtro inválido", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } else {
            fechaDesde = null;
            fechaHasta = null;
        }

        String criterio = (String) filtro.getSelectedItem();
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
        setTitle("Reportes — " + resultado.titulo() + " (" + resultado.filas().size() + " registros)");
    }

    @Override protected void habilitarControles(boolean habilitar) {
        FormPedido.habilitar(getContentPane(), habilitar);
        ReporteServicio.Tipo tipo = (ReporteServicio.Tipo) reporte.getSelectedItem();
        boolean requiereRango = (tipo == ReporteServicio.Tipo.VENTAS_POR_PERIODO
            || tipo == ReporteServicio.Tipo.VENTAS
            || tipo == ReporteServicio.Tipo.MOVIMIENTOS_INVENTARIO
            || tipo == ReporteServicio.Tipo.MOVIMIENTOS_PUNTOS);
        boolean rango = habilitar && requiereRango;
        desde.setEnabled(rango);
        hasta.setEnabled(rango);
        filtro.setEnabled(habilitar && filtro.getItemCount() > 1);
    }
}

package com.panpuntospremios.vista;

import javax.swing.*;
import java.awt.*;

public class VentanaPrincipal extends JFrame {

    public VentanaPrincipal() {
        setTitle("Pan, Puntos y Premios");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        JMenuBar menuBar = new JMenuBar();

        JMenu menuClientes = new JMenu("Clientes");
        JMenuItem itemClientes = new JMenuItem("Gestionar clientes");
        itemClientes.addActionListener(e -> abrirFormClientes());
        menuClientes.add(itemClientes);

        menuBar.add(menuClientes);
        setJMenuBar(menuBar);

        JLabel lblBienvenida = new JLabel("Bienvenido al sistema Pan, Puntos y Premios", SwingConstants.CENTER);
        lblBienvenida.setFont(new Font("Arial", Font.BOLD, 18));
        add(lblBienvenida, BorderLayout.CENTER);
    }

    private void abrirFormClientes() {
        FormClientes form = new FormClientes();
        form.setVisible(true);
    }
}

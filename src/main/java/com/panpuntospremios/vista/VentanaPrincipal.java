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

        JMenu menuCatalogo = new JMenu("Catálogo e inventario");
        JMenuItem itemProductos = new JMenuItem("Gestionar productos");
        JMenuItem itemMenus = new JMenuItem("Gestionar menús");
        JMenuItem itemInventario = new JMenuItem("Inventario y abastecimiento");
        itemProductos.addActionListener(e -> new FormProductos().setVisible(true));
        itemMenus.addActionListener(e -> new FormMenus().setVisible(true));
        itemInventario.addActionListener(e -> new FormInventario().setVisible(true));
        menuCatalogo.add(itemProductos);
        menuCatalogo.add(itemMenus);
        menuCatalogo.addSeparator();
        menuCatalogo.add(itemInventario);

        JMenu menuPedidos = new JMenu("Pedidos");
        JMenuItem itemPedidos = new JMenuItem("Nuevo pedido");
        JMenuItem itemPendientes = new JMenuItem("Pedidos pendientes");
        itemPedidos.addActionListener(e -> new FormPedido().setVisible(true));
        itemPendientes.addActionListener(e -> new FormPedidosPendientes().setVisible(true));
        menuPedidos.add(itemPedidos);
        menuPedidos.add(itemPendientes);
        JMenuItem itemCobro = new JMenuItem("Cobro y comprobantes");
        itemCobro.addActionListener(e -> new FormCobro().setVisible(true));
        menuPedidos.add(itemCobro);

        JMenu menuPuntosYCanjes = new JMenu("Puntos y canjes");
        JMenuItem itemPuntosYCanjes = new JMenuItem("Canjear recompensa");
        itemPuntosYCanjes.addActionListener(e -> new FormPuntosYCanjes().setVisible(true));
        menuPuntosYCanjes.add(itemPuntosYCanjes);

        menuBar.add(menuClientes);
        menuBar.add(menuCatalogo);
        menuBar.add(menuPedidos);
        menuBar.add(menuPuntosYCanjes);
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

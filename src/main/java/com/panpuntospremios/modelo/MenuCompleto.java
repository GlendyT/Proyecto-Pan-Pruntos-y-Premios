package com.panpuntospremios.modelo;

public class MenuCompleto extends ItemVenta {
    private Menu menu;

    public MenuCompleto(Menu menu, int cantidad) {
        super(cantidad, menu.getPrecio());
        this.menu = menu;
    }

    @Override
    public int calcularPuntos() {
        return getCantidad() * 8;
    }

    public Menu getMenu() { return menu; }
}

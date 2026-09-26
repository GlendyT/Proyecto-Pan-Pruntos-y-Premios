package com.panpuntospremios.excepcion;

public class ValidacionException extends IllegalArgumentException {
    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}

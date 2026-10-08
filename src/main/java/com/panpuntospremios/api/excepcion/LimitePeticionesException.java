package com.panpuntospremios.api.excepcion;

public class LimitePeticionesException extends RuntimeException {

    public LimitePeticionesException(String mensaje) {
        super(mensaje);
    }
}

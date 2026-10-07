package com.panpuntospremios.api.dto;

import java.time.Instant;
import java.util.List;

public class ErrorRespuestaDTO {
    private String timestamp;
    private int status;
    private String error;
    private String mensaje;
    private List<String> detalles;

    public ErrorRespuestaDTO() {
        this.timestamp = Instant.now().toString();
    }

    public ErrorRespuestaDTO(int status, String error, String mensaje) {
        this.timestamp = Instant.now().toString();
        this.status = status;
        this.error = error;
        this.mensaje = mensaje;
    }

    public ErrorRespuestaDTO(int status, String error, String mensaje, List<String> detalles) {
        this.timestamp = Instant.now().toString();
        this.status = status;
        this.error = error;
        this.mensaje = mensaje;
        this.detalles = detalles;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public List<String> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<String> detalles) {
        this.detalles = detalles;
    }
}

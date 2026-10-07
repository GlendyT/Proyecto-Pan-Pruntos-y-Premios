package com.panpuntospremios.api.dto;

import jakarta.validation.constraints.NotBlank;

public class ConsumirTokenDTO {

    @NotBlank(message = "El token de acceso es obligatorio")
    private String token;

    public ConsumirTokenDTO() {}

    public ConsumirTokenDTO(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}

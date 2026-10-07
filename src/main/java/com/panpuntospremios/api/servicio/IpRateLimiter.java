package com.panpuntospremios.api.servicio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IpRateLimiter {

    private final int maxSolicitudes;
    private final long ventanaMillis;
    private final Map<String, Deque<Long>> registrosPorIp = new ConcurrentHashMap<>();

    public IpRateLimiter(
            @Value("${app.auth.rate-limit.ip-max-requests:10}") int maxSolicitudes,
            @Value("${app.auth.rate-limit.ip-window-minutes:15}") int ventanaMinutos) {
        this.maxSolicitudes = maxSolicitudes;
        this.ventanaMillis = (long) ventanaMinutos * 60 * 1000L;
    }

    public synchronized boolean permiteSolicitud(String ip) {
        String claveIp = (ip == null || ip.isBlank()) ? "desconocida" : ip.trim();
        long ahora = System.currentTimeMillis();
        long limiteInferior = ahora - ventanaMillis;

        Deque<Long> marcas = registrosPorIp.computeIfAbsent(claveIp, k -> new ArrayDeque<>());

        // Limpiar marcas que ya cayeron fuera de la ventana deslizante
        while (!marcas.isEmpty() && marcas.peekFirst() < limiteInferior) {
            marcas.pollFirst();
        }

        if (marcas.size() >= maxSolicitudes) {
            return false;
        }

        marcas.addLast(ahora);

        // Limpieza periódica de IPs inactivas para evitar acumulación en memoria
        if (registrosPorIp.size() > 1000) {
            registrosPorIp.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        }

        return true;
    }

    public synchronized void resetear() {
        registrosPorIp.clear();
    }
}

package com.panpuntospremios.api.dao;

import com.panpuntospremios.api.modelo.ClienteAuth;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;

@Repository
public class AuthDAOImpl implements AuthDAO {

    private final DataSource dataSource;

    public AuthDAOImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<ClienteAuth> buscarClientePorEmail(String email) {
        String sql = """
            SELECT id_cliente, identificador, nombre, telefono, email, saldo_puntos, activo, email_verificado
              FROM cliente
             WHERE LOWER(email) = LOWER(?)
        """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearCliente(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar cliente por correo", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<ClienteAuth> buscarClientePorId(int idCliente) {
        String sql = """
            SELECT id_cliente, identificador, nombre, telefono, email, saldo_puntos, activo, email_verificado
              FROM cliente
             WHERE id_cliente = ?
        """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearCliente(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar cliente por ID", e);
        }
        return Optional.empty();
    }

    @Override
    public boolean superaLimiteSolicitudes(int idCliente, int cooldownSegundos, int ventanaMinutos, int maxSolicitudes) {
        try (Connection con = dataSource.getConnection()) {
            // 1. Validar Enfriamiento (cooldown): ¿hace cuánto fue la última solicitud?
            String sqlUltima = "SELECT MAX(fecha_creacion) FROM cliente_token_acceso WHERE id_cliente = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlUltima)) {
                ps.setInt(1, idCliente);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Timestamp ultima = rs.getTimestamp(1);
                        if (ultima != null) {
                            long diferenciaSegundos = (System.currentTimeMillis() - ultima.getTime()) / 1000;
                            if (diferenciaSegundos < cooldownSegundos) {
                                return true; // Supera enfriamiento
                            }
                        }
                    }
                }
            }

            // 2. Validar ventana de solicitudes: máximo N solicitudes en X minutos
            long ventanaMillis = System.currentTimeMillis() - ((long) ventanaMinutos * 60 * 1000);
            Timestamp inicioVentana = new Timestamp(ventanaMillis);
            String sqlVentana = "SELECT COUNT(*) FROM cliente_token_acceso WHERE id_cliente = ? AND fecha_creacion >= ?";
            try (PreparedStatement ps = con.prepareStatement(sqlVentana)) {
                ps.setInt(1, idCliente);
                ps.setTimestamp(2, inicioVentana);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        if (count >= maxSolicitudes) {
                            return true; // Supera cuota máxima en la ventana
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al verificar límites de solicitud", e);
        }
        return false;
    }

    @Override
    public void registrarToken(int idCliente, String tokenHash, Timestamp expiracion, String ip, String userAgent) {
        String sql = """
            INSERT INTO cliente_token_acceso (id_cliente, token_hash, fecha_creacion, fecha_expiracion, direccion_ip, origen_solicitud)
            VALUES (?, ?, CURRENT_TIMESTAMP, ?, ?, ?)
        """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            ps.setString(2, tokenHash);
            ps.setTimestamp(3, expiracion);
            ps.setString(4, ip != null && ip.length() > 45 ? ip.substring(0, 45) : ip);
            ps.setString(5, userAgent != null && userAgent.length() > 100 ? userAgent.substring(0, 100) : userAgent);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar token de acceso", e);
        }
    }

    @Override
    public Optional<Integer> consumirToken(String tokenHash) {
        String sqlUpdate = """
            UPDATE cliente_token_acceso
               SET fecha_uso = CURRENT_TIMESTAMP
             WHERE token_hash = ?
               AND fecha_uso IS NULL
               AND fecha_expiracion > CURRENT_TIMESTAMP
        """;
        String sqlSelect = "SELECT id_cliente FROM cliente_token_acceso WHERE token_hash = ?";

        try (Connection con = dataSource.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(sqlUpdate)) {
                ps.setString(1, tokenHash);
                int actualizadas = ps.executeUpdate();
                if (actualizadas == 0) {
                    return Optional.empty(); // Token inexistente, ya usado o expirado
                }
            }

            try (PreparedStatement ps = con.prepareStatement(sqlSelect)) {
                ps.setString(1, tokenHash);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(rs.getInt("id_cliente"));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consumir token de acceso", e);
        }
        return Optional.empty();
    }

    private ClienteAuth mapearCliente(ResultSet rs) throws SQLException {
        boolean verificado = false;
        try {
            verificado = "S".equalsIgnoreCase(rs.getString("email_verificado"));
        } catch (SQLException ignored) {
            // Si la columna no estuviera en alguna base de prueba antigua
        }

        return new ClienteAuth(
            rs.getInt("id_cliente"),
            rs.getString("identificador"),
            rs.getString("nombre"),
            rs.getString("telefono"),
            rs.getString("email"),
            rs.getInt("saldo_puntos"),
            "S".equalsIgnoreCase(rs.getString("activo")),
            verificado
        );
    }
}

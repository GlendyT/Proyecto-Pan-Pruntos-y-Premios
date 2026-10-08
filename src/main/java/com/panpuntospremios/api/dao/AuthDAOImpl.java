package com.panpuntospremios.api.dao;

import com.panpuntospremios.api.modelo.ClienteAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;

@Repository
public class AuthDAOImpl implements AuthDAO {

    private final DataSource dataSource;
    private final String prefijoEsquema;

    public AuthDAOImpl(
            DataSource dataSource,
            @Value("${app.database.schema:}") String esquema) {
        this.dataSource = dataSource;
        this.prefijoEsquema = PrefijoEsquemaSQL.desde(esquema);
    }

    @Override
    public Optional<ClienteAuth> buscarClientePorEmail(String email) {
        String sql = """
            SELECT id_cliente, identificador, nombre, telefono, email, saldo_puntos, activo, email_verificado
              FROM %scliente
             WHERE LOWER(email) = LOWER(?)
        """.formatted(prefijoEsquema);
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
              FROM %scliente
             WHERE id_cliente = ?
        """.formatted(prefijoEsquema);
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
            // Las columnas son TIMESTAMP sin zona horaria. Guardamos y comparamos UTC
            // explícitamente para evitar diferencias entre la sesión Oracle y la JVM.
            boolean h2 = esH2(con);
            String sqlCooldown = """
                SELECT COUNT(*) FROM %scliente_token_acceso
                 WHERE id_cliente = ?
                   AND fecha_creacion >= %s
            """.formatted(prefijoEsquema, h2
                    ? "DATEADD('SECOND', ?, CURRENT_TIMESTAMP)"
                    : "SYS_EXTRACT_UTC(SYSTIMESTAMP) - NUMTODSINTERVAL(?, 'SECOND')");
            try (PreparedStatement ps = con.prepareStatement(sqlCooldown)) {
                ps.setInt(1, idCliente);
                ps.setInt(2, h2 ? -cooldownSegundos : cooldownSegundos);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        if (rs.getInt(1) > 0) {
                            return true;
                        }
                    }
                }
            }

            // Máximo N solicitudes dentro de la ventana calculada por Oracle.
            String sqlVentana = """
                SELECT COUNT(*) FROM %scliente_token_acceso
                 WHERE id_cliente = ?
                   AND fecha_creacion >= %s
            """.formatted(prefijoEsquema, h2
                    ? "DATEADD('MINUTE', ?, CURRENT_TIMESTAMP)"
                    : "SYS_EXTRACT_UTC(SYSTIMESTAMP) - NUMTODSINTERVAL(?, 'MINUTE')");
            try (PreparedStatement ps = con.prepareStatement(sqlVentana)) {
                ps.setInt(1, idCliente);
                ps.setInt(2, h2 ? -ventanaMinutos : ventanaMinutos);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        if (count >= maxSolicitudes) {
                            return true;
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
    public void registrarToken(int idCliente, String tokenHash, int expiracionMinutos, String ip, String userAgent) {
        try (Connection con = dataSource.getConnection()) {
            boolean h2 = esH2(con);
            String ahora = h2 ? "CURRENT_TIMESTAMP" : "SYS_EXTRACT_UTC(SYSTIMESTAMP)";
            String expiracion = h2
                ? "DATEADD('MINUTE', ?, CURRENT_TIMESTAMP)"
                : "SYS_EXTRACT_UTC(SYSTIMESTAMP) + NUMTODSINTERVAL(?, 'MINUTE')";
            String sql = """
                INSERT INTO %scliente_token_acceso (id_cliente, token_hash, fecha_creacion, fecha_expiracion, direccion_ip, origen_solicitud)
                VALUES (?, ?, %s, %s, ?, ?)
            """.formatted(prefijoEsquema, ahora, expiracion);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            ps.setString(2, tokenHash);
            ps.setInt(3, expiracionMinutos);
            ps.setString(4, ip != null && ip.length() > 45 ? ip.substring(0, 45) : ip);
            ps.setString(5, userAgent != null && userAgent.length() > 100 ? userAgent.substring(0, 100) : userAgent);
            ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al registrar token de acceso", e);
        }
    }

    private boolean esH2(Connection con) throws SQLException {
        return "H2".equalsIgnoreCase(con.getMetaData().getDatabaseProductName());
    }

    @Override
    public Optional<Integer> consumirToken(String tokenHash) {
        String sqlUpdate;
        String sqlMarcarVerificado;
        try (Connection con = dataSource.getConnection()) {
            String ahora = esH2(con) ? "CURRENT_TIMESTAMP" : "SYS_EXTRACT_UTC(SYSTIMESTAMP)";
            sqlUpdate = """
                UPDATE %scliente_token_acceso
                   SET fecha_uso = %s
                 WHERE token_hash = ?
                   AND fecha_uso IS NULL
                   AND fecha_expiracion > %s
            """.formatted(prefijoEsquema, ahora, ahora);
            sqlMarcarVerificado = """
                UPDATE %scliente
                   SET email_verificado = 'S',
                       fecha_verificacion_email = CASE
                           WHEN email_verificado = 'S' THEN fecha_verificacion_email
                           ELSE %s
                       END
                 WHERE id_cliente = ?
                   AND activo = 'S'
            """.formatted(prefijoEsquema, ahora);
        } catch (SQLException e) {
            throw new RuntimeException("Error al preparar el consumo del token", e);
        }
        String sqlSelect = "SELECT id_cliente FROM " + prefijoEsquema
            + "cliente_token_acceso WHERE token_hash = ?";

        try (Connection con = dataSource.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(sqlUpdate)) {
                    ps.setString(1, tokenHash);
                    int actualizadas = ps.executeUpdate();
                    if (actualizadas == 0) {
                        con.rollback();
                        return Optional.empty(); // Token inexistente, ya usado o expirado
                    }
                }

                int idCliente;
                try (PreparedStatement ps = con.prepareStatement(sqlSelect)) {
                    ps.setString(1, tokenHash);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return Optional.empty();
                        }
                        idCliente = rs.getInt("id_cliente");
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(sqlMarcarVerificado)) {
                    ps.setInt(1, idCliente);
                    if (ps.executeUpdate() != 1) {
                        // El token valido queda consumido aunque la cuenta ya no este activa.
                        con.commit();
                        return Optional.empty(); // Cuenta inactiva o ya no disponible
                    }
                }

                con.commit();
                return Optional.of(idCliente);
            } catch (SQLException e) {
                try {
                    con.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consumir el token y verificar el correo", e);
        }
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

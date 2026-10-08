package com.panpuntospremios.api.dao;

import com.panpuntospremios.modelo.MovimientoPuntos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class PuntosConsultaDAOImpl implements PuntosConsultaDAO {

    private final DataSource dataSource;
    private final String prefijoEsquema;

    public PuntosConsultaDAOImpl(
            DataSource dataSource,
            @Value("${app.database.schema:}") String esquema) {
        this.dataSource = dataSource;
        this.prefijoEsquema = PrefijoEsquemaSQL.desde(esquema);
    }

    @Override
    public Optional<ClienteResumen> buscarClienteActivo(int idCliente) throws SQLException {
        String sql = """
            SELECT id_cliente, nombre, saldo_puntos
              FROM %scliente
             WHERE id_cliente = ?
               AND activo = 'S'
        """.formatted(prefijoEsquema);

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new ClienteResumen(
                        rs.getInt("id_cliente"),
                        rs.getString("nombre"),
                        rs.getInt("saldo_puntos")
                    ));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<MovimientoPuntos> listarMovimientos(
            int idCliente,
            Timestamp antesDe,
            Integer antesDeId,
            int limite) throws SQLException {
        boolean conCursor = antesDe != null && antesDeId != null;
        String sql = conCursor
            ? """
                SELECT id_movimiento, id_cliente, fecha, tipo, puntos, referencia
                  FROM %smovimiento_puntos
                 WHERE id_cliente = ?
                   AND (fecha < ? OR (fecha = ? AND id_movimiento < ?))
                 ORDER BY fecha DESC, id_movimiento DESC
                FETCH FIRST ? ROWS ONLY
                """.formatted(prefijoEsquema)
            : """
                SELECT id_movimiento, id_cliente, fecha, tipo, puntos, referencia
                  FROM %smovimiento_puntos
                 WHERE id_cliente = ?
                 ORDER BY fecha DESC, id_movimiento DESC
                 FETCH FIRST ? ROWS ONLY
                """.formatted(prefijoEsquema);

        List<MovimientoPuntos> movimientos = new ArrayList<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            if (conCursor) {
                ps.setTimestamp(2, antesDe);
                ps.setTimestamp(3, antesDe);
                ps.setInt(4, antesDeId);
                ps.setInt(5, limite);
            } else {
                ps.setInt(2, limite);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    movimientos.add(new MovimientoPuntos(
                        rs.getInt("id_movimiento"),
                        rs.getInt("id_cliente"),
                        rs.getTimestamp("fecha"),
                        rs.getString("tipo"),
                        rs.getInt("puntos"),
                        rs.getString("referencia")
                    ));
                }
            }
        }
        return movimientos;
    }
}

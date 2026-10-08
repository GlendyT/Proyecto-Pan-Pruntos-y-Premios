package com.panpuntospremios.api.servicio;

import com.panpuntospremios.api.dao.PuntosConsultaDAO;
import com.panpuntospremios.api.dto.MiPerfilDTO;
import com.panpuntospremios.api.dto.MovimientoPuntosDTO;
import com.panpuntospremios.api.dto.PaginaMovimientosDTO;
import com.panpuntospremios.api.dto.SaldoPuntosDTO;
import com.panpuntospremios.api.modelo.ClientePrincipal;
import com.panpuntospremios.excepcion.ValidacionException;
import com.panpuntospremios.modelo.MovimientoPuntos;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class PuntosConsultaService {

    private static final int LIMITE_POR_DEFECTO = 20;
    private static final int LIMITE_MAXIMO = 100;

    private final PuntosConsultaDAO puntosDAO;

    public PuntosConsultaService(PuntosConsultaDAO puntosDAO) {
        this.puntosDAO = puntosDAO;
    }

    public MiPerfilDTO obtenerPerfil(ClientePrincipal principal) throws SQLException {
        PuntosConsultaDAO.ClienteResumen cliente = clienteActivo(principal.getIdCliente());
        return new MiPerfilDTO(cliente.idCliente(), cliente.nombre());
    }

    public SaldoPuntosDTO obtenerSaldo(ClientePrincipal principal) throws SQLException {
        PuntosConsultaDAO.ClienteResumen cliente = clienteActivo(principal.getIdCliente());
        return new SaldoPuntosDTO(cliente.saldoPuntos());
    }

    public PaginaMovimientosDTO listarMovimientos(
            ClientePrincipal principal,
            Integer limiteSolicitado,
            String cursor) throws SQLException {
        int limite = limiteSolicitado == null ? LIMITE_POR_DEFECTO : limiteSolicitado;
        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw new ValidacionException("El límite debe estar entre 1 y " + LIMITE_MAXIMO + ".");
        }

        int idCliente = principal.getIdCliente();
        clienteActivo(idCliente);
        CursorPuntos cursorDecodificado = decodificarCursor(cursor);
        List<MovimientoPuntos> encontrados = puntosDAO.listarMovimientos(
            idCliente,
            cursorDecodificado == null ? null : cursorDecodificado.fecha(),
            cursorDecodificado == null ? null : cursorDecodificado.idMovimiento(),
            limite + 1
        );

        boolean hayMas = encontrados.size() > limite;
        if (hayMas) {
            encontrados = new ArrayList<>(encontrados.subList(0, limite));
        }

        List<MovimientoPuntosDTO> respuesta = encontrados.stream()
            .map(this::aDTO)
            .toList();

        String siguienteCursor = null;
        if (hayMas && !encontrados.isEmpty()) {
            MovimientoPuntos ultimo = encontrados.get(encontrados.size() - 1);
            siguienteCursor = codificarCursor(ultimo);
        }

        return new PaginaMovimientosDTO(respuesta, siguienteCursor, hayMas);
    }

    private PuntosConsultaDAO.ClienteResumen clienteActivo(int idCliente) throws SQLException {
        return puntosDAO.buscarClienteActivo(idCliente)
            .orElseThrow(() -> new ClienteNoDisponibleException());
    }

    private MovimientoPuntosDTO aDTO(MovimientoPuntos movimiento) {
        Instant fecha = movimiento.getFecha() == null
            ? null
            : movimiento.getFecha().toInstant();
        return new MovimientoPuntosDTO(
            movimiento.getIdMovimiento(),
            fecha,
            movimiento.getTipo(),
            movimiento.getPuntos(),
            movimiento.getReferencia()
        );
    }

    private String codificarCursor(MovimientoPuntos movimiento) {
        long epochMillis = movimiento.getFecha().getTime();
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES + Integer.BYTES);
        buffer.putLong(epochMillis);
        buffer.putInt(movimiento.getIdMovimiento());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.array());
    }

    private CursorPuntos decodificarCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {
            if (cursor.length() > 64) {
                throw new IllegalArgumentException("Cursor demasiado largo");
            }
            byte[] bytes = Base64.getUrlDecoder().decode(cursor);
            if (bytes.length != Long.BYTES + Integer.BYTES) {
                throw new IllegalArgumentException("Tamaño de cursor inválido");
            }
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            Timestamp fecha = new Timestamp(buffer.getLong());
            int idMovimiento = buffer.getInt();
            if (idMovimiento <= 0) {
                throw new IllegalArgumentException("ID de cursor inválido");
            }
            return new CursorPuntos(fecha, idMovimiento);
        } catch (IllegalArgumentException ex) {
            throw new ValidacionException("El cursor de paginación no es válido.");
        }
    }

    private record CursorPuntos(Timestamp fecha, int idMovimiento) {
    }

    public static class ClienteNoDisponibleException extends RuntimeException {
        public ClienteNoDisponibleException() {
            super("No se encontró un cliente activo para la sesión.");
        }
    }
}

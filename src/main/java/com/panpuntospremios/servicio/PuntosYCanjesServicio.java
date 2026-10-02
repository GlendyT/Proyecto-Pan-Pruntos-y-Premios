package com.panpuntospremios.servicio;

import com.panpuntospremios.dao.*;
import com.panpuntospremios.excepcion.*;
import com.panpuntospremios.modelo.*;
import java.sql.*;
import java.util.*;

public class PuntosYCanjesServicio {
    private final Connection conexion;
    private final RecompensaDAO recompensas;
    private final CanjeDAO canjes;
    private final ProductoDAO productos;
    private final MovimientoInventarioDAO movInventario;
    private final MovimientoPuntosDAO movPuntos;
    private final PedidoDAO pedidos;

    public PuntosYCanjesServicio(Connection conexion) {
        this.conexion = conexion;
        recompensas = new RecompensaDAOImpl(conexion);
        canjes = new CanjeDAOImpl(conexion);
        productos = new ProductoDAOImpl(conexion);
        movInventario = new MovimientoInventarioDAOImpl(conexion);
        movPuntos = new MovimientoPuntosDAOImpl(conexion);
        pedidos = new PedidoDAOImpl(conexion);
    }

    // ── Catálogo de recompensas ──────────────────────────────────────────────

    public List<Recompensa> listarRecompensasActivas() throws SQLException {
        return recompensas.listarActivas();
    }

    public void registrarRecompensa(Recompensa r) throws SQLException {
        validarRecompensa(r);
        recompensas.registrar(r);
    }

    public void actualizarRecompensa(Recompensa r) throws SQLException {
        validarRecompensa(r);
        if (r.getIdRecompensa() <= 0) throw new ValidacionException("ID de recompensa inválido.");
        recompensas.actualizar(r);
    }

    public void desactivarRecompensa(int idRecompensa) throws SQLException {
        if (idRecompensa <= 0) throw new ValidacionException("ID de recompensa inválido.");
        recompensas.desactivar(idRecompensa);
    }

    // ── Historial de puntos ──────────────────────────────────────────────────

    public List<MovimientoPuntos> listarMovimientosPuntos(int idCliente) throws SQLException {
        return movPuntos.listarPorCliente(idCliente);
    }

    public List<Pedido> listarPedidosPorCliente(int idCliente) throws SQLException {
        return pedidos.listarPorCliente(idCliente);
    }

    public List<Canje> listarCanjesPorCliente(int idCliente) throws SQLException {
        return canjes.listarPorCliente(idCliente);
    }

    // ── Canje ────────────────────────────────────────────────────────────────

    public void canjear(int idCliente, int idRecompensa)
            throws SQLException, PuntosInsuficientesException, StockInsuficienteException {
        if (!conexion.getAutoCommit()) throw new ValidacionException("Ya existe una transacción en curso.");
        conexion.setAutoCommit(false);
        try {
            int saldo;
            try (PreparedStatement ps = conexion.prepareStatement(
                    "SELECT saldo_puntos, activo FROM cliente WHERE id_cliente = ? FOR UPDATE")) {
                ps.setInt(1, idCliente);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || !"S".equals(rs.getString("activo")))
                        throw new ValidacionException("El cliente no existe o está inactivo.");
                    saldo = rs.getInt("saldo_puntos");
                }
            }

            Recompensa recompensa = recompensas.buscarPorId(idRecompensa);
            if (recompensa == null || !recompensa.isActivo())
                throw new ValidacionException("La recompensa no existe o está inactiva.");

            if (saldo < recompensa.getPuntosNecesarios())
                throw new PuntosInsuficientesException(
                    "Puntos insuficientes. Saldo: " + saldo +
                    ", necesarios: " + recompensa.getPuntosNecesarios() + ".");

            Map<Integer, Integer> requerido = new TreeMap<>();
            if (recompensa.getIdProducto() != null) {
                requerido.put(recompensa.getIdProducto(), 1);
            } else {
                String codigoMenu;
                try (PreparedStatement ps = conexion.prepareStatement(
                        "SELECT codigo FROM menu WHERE id_menu = ? AND activo = 'S'")) {
                    ps.setInt(1, recompensa.getIdMenu());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new ValidacionException("El menú de la recompensa no está disponible.");
                        codigoMenu = rs.getString("codigo");
                    }
                }
                Menu menu = new MenuDAOImpl(conexion).buscarPorCodigo(codigoMenu);
                for (Producto p : menu.getComponentes()) requerido.put(p.getIdProducto(), 1);
            }

            for (var entrada : requerido.entrySet()) {
                Producto producto = productos.bloquearPorId(entrada.getKey());
                if (producto == null || !producto.isActivo())
                    throw new ValidacionException("Producto de la recompensa no disponible.");
                if (producto.getExistencia() < entrada.getValue())
                    throw new StockInsuficienteException("Stock insuficiente para " + producto.getNombre() + ".");
                int restante = producto.getExistencia() - entrada.getValue();
                productos.actualizarExistencia(producto.getIdProducto(), restante);
                movInventario.registrar(new MovimientoInventario(
                    0, producto.getIdProducto(), null, "CANJE", entrada.getValue(), restante));
            }

            try (PreparedStatement ps = conexion.prepareStatement(
                    "UPDATE cliente SET saldo_puntos = saldo_puntos - ? WHERE id_cliente = ?")) {
                ps.setInt(1, recompensa.getPuntosNecesarios());
                ps.setInt(2, idCliente);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conexion.prepareStatement(
                    "INSERT INTO movimiento_puntos (id_cliente, tipo, puntos, referencia) VALUES (?, 'CANJE', ?, ?)")) {
                ps.setInt(1, idCliente);
                ps.setInt(2, recompensa.getPuntosNecesarios());
                ps.setString(3, "CANJE:" + idRecompensa);
                ps.executeUpdate();
            }

            canjes.registrar(idCliente, idRecompensa, recompensa.getPuntosNecesarios());
            conexion.commit();
        } catch (SQLException | PuntosInsuficientesException | StockInsuficienteException | RuntimeException e) {
            try { conexion.rollback(); } catch (SQLException rb) { e.addSuppressed(rb); }
            throw e;
        } finally {
            conexion.setAutoCommit(true);
        }
    }

    // ── Validación interna ───────────────────────────────────────────────────

    private void validarRecompensa(Recompensa r) {
        if (r == null) throw new ValidacionException("Los datos de la recompensa son obligatorios.");
        if (r.getNombre() == null || r.getNombre().isBlank())
            throw new ValidacionException("El nombre de la recompensa es obligatorio.");
        if (r.getNombre().length() > 100)
            throw new ValidacionException("El nombre no puede superar 100 caracteres.");
        if (r.getPuntosNecesarios() <= 0)
            throw new ValidacionException("Los puntos necesarios deben ser mayores que cero.");
        boolean tieneProducto = r.getIdProducto() != null;
        boolean tieneMenu = r.getIdMenu() != null;
        if (tieneProducto == tieneMenu)
            throw new ValidacionException("La recompensa debe asociarse a un producto o a un menú, no a ambos ni a ninguno.");
    }
}

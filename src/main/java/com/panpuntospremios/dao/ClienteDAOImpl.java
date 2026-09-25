package com.panpuntospremios.dao;

import com.panpuntospremios.modelo.Cliente;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAOImpl implements ClienteDAO {
    private final Connection conexion;

    public ClienteDAOImpl(Connection conexion) {
        this.conexion = conexion;
    }

    @Override
    public void registrar(Cliente cliente) throws SQLException {
        String sql = "INSERT INTO cliente (identificador, nombre, telefono, email) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, cliente.getIdentificador());
            ps.setString(2, cliente.getNombre());
            ps.setString(3, cliente.getTelefono());
            ps.setString(4, cliente.getEmail());
            ps.executeUpdate();
        }
    }

    @Override
    public Cliente buscarPorIdentificador(String identificador) throws SQLException {
        String sql = "SELECT id_cliente, identificador, nombre, telefono, email, saldo_puntos, activo, fecha_registro FROM cliente WHERE identificador = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, identificador);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapear(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<Cliente> listarActivos() throws SQLException {
        String sql = "SELECT id_cliente, identificador, nombre, telefono, email, saldo_puntos, activo, fecha_registro FROM cliente WHERE activo = 'S' ORDER BY nombre";
        List<Cliente> lista = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        }
        return lista;
    }

    @Override
    public void actualizar(Cliente cliente) throws SQLException {
        String sql = "UPDATE cliente SET nombre = ?, telefono = ?, email = ? WHERE id_cliente = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getTelefono());
            ps.setString(3, cliente.getEmail());
            ps.setInt(4, cliente.getIdCliente());
            ps.executeUpdate();
        }
    }

    @Override
    public void desactivar(int idCliente) throws SQLException {
        String sql = "UPDATE cliente SET activo = 'N' WHERE id_cliente = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            ps.executeUpdate();
        }
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        return new Cliente(
            rs.getInt("id_cliente"),
            rs.getString("identificador"),
            rs.getString("nombre"),
            rs.getString("telefono"),
            rs.getString("email"),
            rs.getInt("saldo_puntos"),
            "S".equals(rs.getString("activo")),
            rs.getDate("fecha_registro")
        );
    }
}

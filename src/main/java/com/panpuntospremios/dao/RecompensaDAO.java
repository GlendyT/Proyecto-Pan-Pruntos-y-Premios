package com.panpuntospremios.dao;

import com.panpuntospremios.modelo.Recompensa;
import java.sql.SQLException;
import java.util.List;

public interface RecompensaDAO {
    List<Recompensa> listarActivas() throws SQLException;
    Recompensa buscarPorId(int idRecompensa) throws SQLException;
    void registrar(Recompensa recompensa) throws SQLException;
    void actualizar(Recompensa recompensa) throws SQLException;
    void desactivar(int idRecompensa) throws SQLException;
}

package com.panpuntospremios.vista;

import com.panpuntospremios.util.ConexionOracle;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import java.awt.Cursor;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

abstract class FormularioAsincrono extends JFrame {
    private volatile Connection conexion;
    private SwingWorker<?, ?> tareaActual;
    private volatile boolean cerrado;

    protected FormularioAsincrono() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                cerrarRecursos();
            }
        });
    }

    protected Connection abrirConexion() throws Exception {
        Connection nuevaConexion = ConexionOracle.obtener();
        conexion = nuevaConexion;
        if (cerrado) {
            nuevaConexion.close();
            conexion = null;
            throw new CancellationException();
        }
        return nuevaConexion;
    }

    protected <T> void ejecutar(
        Callable<T> operacion,
        Consumer<T> alCompletar,
        String mensajeError
    ) {
        cambiarEstado(true);
        tareaActual = new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return operacion.call();
            }

            @Override
            protected void done() {
                try {
                    if (!cerrado) {
                        alCompletar.accept(get());
                    }
                } catch (CancellationException ignored) {
                    // La ventana fue cerrada durante la operación.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mostrarError(mensajeError, e);
                } catch (ExecutionException e) {
                    mostrarError(mensajeError, e.getCause());
                } finally {
                    tareaActual = null;
                    cambiarEstado(false);
                }
            }
        };
        tareaActual.execute();
    }

    protected abstract void habilitarControles(boolean habilitar);

    private void cambiarEstado(boolean operando) {
        habilitarControles(!operando && !cerrado);
        setCursor(Cursor.getPredefinedCursor(operando ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void mostrarError(String contexto, Throwable error) {
        if (cerrado) {
            return;
        }
        String detalle = error == null || error.getMessage() == null || error.getMessage().isBlank()
            ? "Consulta el registro de la aplicación."
            : error.getMessage();
        JOptionPane.showMessageDialog(
            this,
            contexto + System.lineSeparator() + detalle,
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
    }

    private void cerrarRecursos() {
        cerrado = true;
        if (tareaActual != null) {
            tareaActual.cancel(true);
        }
        if (conexion != null) {
            try {
                conexion.close();
            } catch (Exception ignored) {
                // La ventana ya está cerrada y no se puede mostrar el error.
            }
        }
    }
}

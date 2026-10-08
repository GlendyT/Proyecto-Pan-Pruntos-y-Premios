package com.panpuntospremios.api.excepcion;

import com.panpuntospremios.api.dto.ErrorRespuestaDTO;
import com.panpuntospremios.api.servicio.PuntosConsultaService;
import com.panpuntospremios.excepcion.PagoRechazadoException;
import com.panpuntospremios.excepcion.PuntosInsuficientesException;
import com.panpuntospremios.excepcion.StockInsuficienteException;
import com.panpuntospremios.excepcion.ValidacionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LimitePeticionesException.class)
    public ResponseEntity<ErrorRespuestaDTO> manejarLimitePeticiones(LimitePeticionesException ex) {
        ErrorRespuestaDTO error = new ErrorRespuestaDTO(
            HttpStatus.TOO_MANY_REQUESTS.value(),
            HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorRespuestaDTO> manejarBadCredentials(BadCredentialsException ex) {
        ErrorRespuestaDTO error = new ErrorRespuestaDTO(
            HttpStatus.UNAUTHORIZED.value(),
            HttpStatus.UNAUTHORIZED.getReasonPhrase(),
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(ValidacionException.class)
    public ResponseEntity<ErrorRespuestaDTO> manejarValidacion(ValidacionException ex) {
        ErrorRespuestaDTO error = new ErrorRespuestaDTO(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<ErrorRespuestaDTO> manejarStockInsuficiente(StockInsuficienteException ex) {
        ErrorRespuestaDTO error = new ErrorRespuestaDTO(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(PuntosInsuficientesException.class)
    public ResponseEntity<ErrorRespuestaDTO> manejarPuntosInsuficientes(PuntosInsuficientesException ex) {
        ErrorRespuestaDTO error = new ErrorRespuestaDTO(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(PagoRechazadoException.class)
    public ResponseEntity<ErrorRespuestaDTO> manejarPagoRechazado(PagoRechazadoException ex) {
        ErrorRespuestaDTO error = new ErrorRespuestaDTO(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuestaDTO> manejarArgumentoInvalido(MethodArgumentNotValidException ex) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.toList());

        ErrorRespuestaDTO error = new ErrorRespuestaDTO(
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            "Error en los parámetros enviados en la solicitud",
            detalles
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorRespuestaDTO> manejarRecursoNoEncontrado(NoResourceFoundException ex) {
        ErrorRespuestaDTO error = new ErrorRespuestaDTO(
            HttpStatus.NOT_FOUND.value(),
            HttpStatus.NOT_FOUND.getReasonPhrase(),
            "El recurso solicitado no existe"
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(PuntosConsultaService.ClienteNoDisponibleException.class)
    public ResponseEntity<ErrorRespuestaDTO> manejarClienteNoDisponible(
            PuntosConsultaService.ClienteNoDisponibleException ex) {
        ErrorRespuestaDTO error = new ErrorRespuestaDTO(
            HttpStatus.NOT_FOUND.value(),
            HttpStatus.NOT_FOUND.getReasonPhrase(),
            "No se encontró un cliente activo para la sesión"
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorRespuestaDTO> manejarMetodoNoSoportado(HttpRequestMethodNotSupportedException ex) {
        ErrorRespuestaDTO error = new ErrorRespuestaDTO(
            HttpStatus.METHOD_NOT_ALLOWED.value(),
            HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase(),
            "Método HTTP no permitido para esta ruta"
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ErrorRespuestaDTO> manejarSqlException(SQLException ex) {
        // Por seguridad, nunca se retorna el mensaje técnico de la BD ni trazas SQL al cliente
        ErrorRespuestaDTO error = new ErrorRespuestaDTO(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
            "Ocurrió un error interno al consultar la base de datos"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuestaDTO> manejarExcepcionGeneral(Exception ex) {
        ErrorRespuestaDTO error = new ErrorRespuestaDTO(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
            "Ocurrió un error inesperado en el servidor"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

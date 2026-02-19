package com.utn.gestion_de_turnos.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GoogleCalendarException.class)
    public ResponseEntity<?> handleGoogleCalendarException(GoogleCalendarException ex) {
        log.error("Error de Google Calendar: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "Hubo un problema con el calendario. Intenta nuevamente."));
    }

    @ExceptionHandler(TiempoDeReservaOcupadoException.class)
    public ResponseEntity<?> handleTiempoDeReservaOcupado(TiempoDeReservaOcupadoException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ReservaNotFoundException.class)
    public ResponseEntity<?> handleReservaNotFound(ReservaNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(AccesoProhibidoException.class)
    public ResponseEntity<?> handleAccesoProhibido(AccesoProhibidoException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ReservaNoCancelableException.class)
    public ResponseEntity<?> handleReservaNoCancelable(ReservaNoCancelableException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<?> handleSecurity(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntime(RuntimeException ex) {
        log.error("Error inesperado (RuntimeException): {}", ex.getMessage(), ex);
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Ocurrió un error inesperado. Intenta nuevamente."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneralException(Exception ex) {
        log.error("Error inesperado (Exception): {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Ocurrió un error inesperado. Intenta nuevamente."));
    }

    @ExceptionHandler(SalaNotFoundException.class)
    public ResponseEntity<?> handleSalaNotFound(SalaNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(SalaConReservasActivasException.class)
    public ResponseEntity<?> handleSalaConReservasActivas(SalaConReservasActivasException ex) {
        // 409 = conflicto de negocio (regla del sistema)
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }


}

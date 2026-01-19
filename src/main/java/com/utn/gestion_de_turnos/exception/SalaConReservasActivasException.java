package com.utn.gestion_de_turnos.exception;

/**
 * Se lanza cuando se intenta eliminar (borrado lógico) una sala que tiene al menos
 * una reserva en estado ACTIVO.
 */
public class SalaConReservasActivasException extends RuntimeException {

    public SalaConReservasActivasException(String message) {
        super(message);
    }
}

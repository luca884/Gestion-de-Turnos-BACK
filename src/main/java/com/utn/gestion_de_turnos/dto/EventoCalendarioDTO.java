package com.utn.gestion_de_turnos.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventoCalendarioDTO {
    private Long id;
    private LocalDateTime start;
    private LocalDateTime end;
    private String title;
    private String description;
    /**
     * Estado de la reserva (ACTIVO / FINALIZADO / CANCELADO).
     *
     * Nota: agregar este campo NO rompe el frontend existente (si no lo usa, se ignora),
     * pero te permite pintar distinto en el calendario según el estado.
     */
    private String estado;
}

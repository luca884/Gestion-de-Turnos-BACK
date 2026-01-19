package com.utn.gestion_de_turnos.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "salas")
public class Sala {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SalaSize salaSize;

    @JsonProperty("cantidad_personas")
    @Column(name = "cantidad_personas", nullable = false)
    private int cantPersonas;

    @Column(length = 255)
    private String descripcion;

    /**
     * Borrado lógico.
     *
     * - true  => sala “eliminada” (no se lista ni se usa para nuevas reservas)
     * - false => sala activa (uso normal)
     *
     * Motivo: conservar historial de reservas sin romper la FK (turnos.sala_id).
     */
    @Column(nullable = false)
    private boolean eliminada = false;

    public void setSalaSize(SalaSize salaSize) {
        this.salaSize = salaSize;
        if (salaSize != null) {
            // Mantiene consistente la capacidad máxima según el tamaño de la sala.
            this.cantPersonas = salaSize.getCapacidadMaxima();
        }
    }
}

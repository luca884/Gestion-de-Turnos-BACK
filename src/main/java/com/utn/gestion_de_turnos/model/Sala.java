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

    public void setSalaSize(SalaSize salaSize) {
        this.salaSize = salaSize;
        if (salaSize != null) {
            this.cantPersonas = salaSize.getCapacidadMaxima();
        }
    }
}
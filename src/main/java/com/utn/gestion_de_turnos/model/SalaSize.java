package com.utn.gestion_de_turnos.model;

public enum SalaSize {
    PEQUEÑA(10),
    MEDIANA(25),
    GRANDE(40);
    
    private final int capacidadMaxima;
    
    SalaSize(int capacidadMaxima){
        this.capacidadMaxima = capacidadMaxima;
    }

    public int getCapacidadMaxima() {
            return capacidadMaxima;
    }
}

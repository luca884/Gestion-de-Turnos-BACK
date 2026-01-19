package com.utn.gestion_de_turnos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // ✅ Habilita tareas programadas (usado para finalizar reservas vencidas automáticamente)
public class GestionDeTurnosApplication {
	public static void main(String[] args) {
		SpringApplication.run(GestionDeTurnosApplication.class, args);
	}
}

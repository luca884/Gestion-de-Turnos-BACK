package com.utn.gestion_de_turnos.scheduler;

import com.utn.gestion_de_turnos.service.ReservaService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservaFinalizadorScheduler {

    private final ReservaService reservaService;

    /**
     * Ejecuta el finalizador cada 60 segundos.
     */
    @Scheduled(fixedDelay = 60_000)
    public void finalizarReservasVencidas() {
        // UPDATE directo en DB (eficiente): ACTIVO -> FINALIZADO cuando fechaFinal <= ahora
        reservaService.finalizarReservasVencidas();
    }
}

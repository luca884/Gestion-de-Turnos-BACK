package com.utn.gestion_de_turnos.repository;

import com.utn.gestion_de_turnos.model.Sala;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SalaRepository extends JpaRepository<Sala, Long> {

    /**
     * Devuelve solo salas NO eliminadas (borrado lógico = false).
     */
    List<Sala> findAllByEliminadaFalse();

    /**
     * Busca una sala por id solo si NO está eliminada.
     * Evita reservar/editar salas eliminadas.
     */
    Optional<Sala> findByIdAndEliminadaFalse(Long id);

    /**
     * Salas disponibles entre fechas:
     * - Solo considera conflictos con reservas ACTIVAS.
     * - Ignora reservas CANCELADAS o FINALIZADAS (historial).
     * - Excluye salas eliminadas.
     */
    @Query("""
            SELECT s
            FROM Sala s
            WHERE s.eliminada = false
              AND NOT EXISTS (
                SELECT r.id
                FROM Reserva r
                WHERE r.sala = s
                  AND r.estado = com.utn.gestion_de_turnos.model.Reserva.Estado.ACTIVO
                  AND r.fechaInicio < :fechaFinal
                  AND r.fechaFinal > :fechaInicio
              )
            """)
    List<Sala> findSalasDisponibles(@Param("fechaInicio") LocalDateTime fechaInicio,
                                    @Param("fechaFinal") LocalDateTime fechaFinal);
}

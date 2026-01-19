package com.utn.gestion_de_turnos.repository;

import com.utn.gestion_de_turnos.model.Reserva;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByEstado(Reserva.Estado estado);

    List<Reserva> findByClienteId(Long clienteId);

    List<Reserva> findByClienteIdAndEstado(Long clienteId, Reserva.Estado estado);

    List<Reserva> findBySalaId(Long salaId);

    List<Reserva> findAll(Specification<Reserva> spec);

    /**
     * Devuelve reservas cuyos estados estén dentro del conjunto indicado.
     * Útil para el calendario (ACTIVO + FINALIZADO), sin incluir CANCELADO.
     */
    List<Reserva> findByEstadoIn(List<Reserva.Estado> estados);


    Optional<Reserva> findByGoogleEventId(String eventId);


    /**
     * Reservas ACTIVAS cuyo fin ya pasó (<= ahora).
     * Se usa para pasar de ACTIVO -> FINALIZADO y luego actualizar Google Calendar.
     */
    List<Reserva> findByEstadoAndFechaFinalLessThanEqual(Reserva.Estado estado, LocalDateTime fechaFinal);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Reserva r
           SET r.estado = com.utn.gestion_de_turnos.model.Reserva.Estado.FINALIZADO
         WHERE r.estado = com.utn.gestion_de_turnos.model.Reserva.Estado.ACTIVO
           AND r.fechaFinal <= :ahora
        """)
    int finalizarReservasVencidas(@Param("ahora") LocalDateTime ahora);


    /**
     * Busca reservas ACTIVAS que se superpongan con el rango.
     * (CANCELADO/FINALIZADO no deben bloquear turnos nuevos)
     */
    @Query("""
            SELECT t
            FROM Reserva t
            WHERE t.sala.id = :salaId
              AND t.estado = com.utn.gestion_de_turnos.model.Reserva.Estado.ACTIVO
              AND (:fechaInicio < t.fechaFinal AND :fechaFinal > t.fechaInicio)
            """)
    List<Reserva> findConflictingReservas(@Param("salaId") Long salaId,
                                          @Param("fechaInicio") LocalDateTime fechaInicio,
                                          @Param("fechaFinal") LocalDateTime fechaFinal);

    @Query("SELECT r FROM Reserva r WHERE r.sala.id = :salaId AND r.estado = :estado")
    List<Reserva> findBySalaIdAndEstado(@Param("salaId") Long salaId,
                                        @Param("estado") Reserva.Estado estado);

    /**
     * Chequeo rápido: ¿existe al menos una reserva con ese estado para esa sala?
     * Se usa para validar “no se puede eliminar sala si tiene reservas ACTIVAS”.
     */
    boolean existsBySalaIdAndEstado(Long salaId, Reserva.Estado estado);






}

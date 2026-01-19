package com.utn.gestion_de_turnos.service;

import com.utn.gestion_de_turnos.exception.SalaConReservasActivasException;
import com.utn.gestion_de_turnos.exception.SalaNotFoundException;
import com.utn.gestion_de_turnos.model.Sala;
import com.utn.gestion_de_turnos.repository.SalaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SalaService {

    @Autowired
    private SalaRepository salaRepository;

    @Autowired
    private ReservaService reservaService;

    public SalaService(SalaRepository salaRepository) {
        this.salaRepository = salaRepository;
    }

    /**
     * Crea o actualiza una sala.
     *
     * Nota: Para evitar “revivir” una sala eliminada por error, cuando viene con id
     * primero validamos que exista y no esté eliminada.
     */
    public Sala save(Sala sala) {
        if (sala.getId() == null) {
            // Alta normal: si no viene el flag, queda en false por defecto.
            sala.setEliminada(false);
            return salaRepository.save(sala);
        }

        // Update: solo permitimos actualizar una sala si NO está eliminada.
        Sala existente = salaRepository.findByIdAndEliminadaFalse(sala.getId())
                .orElseThrow(() -> new SalaNotFoundException("Sala no encontrada"));

        // Copiamos los campos editables.
        existente.setNumero(sala.getNumero());
        existente.setSalaSize(sala.getSalaSize());
        existente.setCantPersonas(sala.getCantPersonas());
        existente.setDescripcion(sala.getDescripcion());

        return salaRepository.save(existente);
    }

    /**
     * Busca una sala por id, pero tratando las salas eliminadas como “no existentes”.
     */
    public Optional<Sala> findById(Long id) {
        return salaRepository.findByIdAndEliminadaFalse(id);
    }

    /**
     * Lista de salas visible para uso normal (solo NO eliminadas).
     */
    public List<Sala> findAll() {
        return salaRepository.findAllByEliminadaFalse();
    }

    /**
     * “Eliminar” sala = BORRADO LÓGICO.
     *
     * Regla:
     * - Si tiene reservas ACTIVAS -> error (no se puede eliminar).
     * - Si solo tiene reservas CANCELADAS/FINALIZADAS -> se permite.
     *
     * Motivo técnico:
     * No podemos borrar físicamente la fila porque el historial (tabla turnos)
     * tiene una FK obligatoria a sala_id.
     */
    @Transactional
    public void deleteById(Long id) {
        Sala sala = salaRepository.findByIdAndEliminadaFalse(id)
                .orElseThrow(() -> new SalaNotFoundException("Sala no encontrada"));

        if (reservaService.existsActiveReservaForSala(id)) {
            throw new SalaConReservasActivasException(
                    "No se puede eliminar la sala porque tiene reservas activas."
            );
        }

        // Soft delete: se oculta del sistema, pero queda para el historial.
        sala.setEliminada(true);
        salaRepository.save(sala);
    }

    /**
     * Devuelve salas libres en un rango horario.
     * (Ignora reservas canceladas/finalizadas y salas eliminadas.)
     */
    public List<Sala> encontrarSalasDisponibles(LocalDateTime fechaInicio, LocalDateTime fechaFinal) {
        return salaRepository.findSalasDisponibles(fechaInicio, fechaFinal);
    }

    /**
     * Endpoint auxiliar para el front: “¿Se puede eliminar esta sala?”
     *
     * true  -> no tiene reservas ACTIVAS.
     * false -> tiene al menos una ACTIVA, o la sala no existe / ya está eliminada.
     */
    public boolean canDeleteSala(Long salaId) {
        if (salaRepository.findByIdAndEliminadaFalse(salaId).isEmpty()) {
            return false;
        }
        return !reservaService.existsActiveReservaForSala(salaId);
    }
}

package com.utn.gestion_de_turnos.service;

import com.google.api.services.calendar.model.Event;
import com.utn.gestion_de_turnos.API_Calendar.Service.GoogleCalendarService;
import com.utn.gestion_de_turnos.exception.AccesoProhibidoException;
import com.utn.gestion_de_turnos.exception.ReservaNoCancelableException;
import com.utn.gestion_de_turnos.exception.ReservaNotFoundException;
import com.utn.gestion_de_turnos.exception.TiempoDeReservaOcupadoException;
import com.utn.gestion_de_turnos.model.Cliente;
import com.utn.gestion_de_turnos.model.Reserva;
import com.utn.gestion_de_turnos.model.Sala;
import com.utn.gestion_de_turnos.model.Usuario;
import com.utn.gestion_de_turnos.repository.ClienteRepository;
import com.utn.gestion_de_turnos.repository.ReservaRepository;
import com.utn.gestion_de_turnos.repository.SalaRepository;
import com.utn.gestion_de_turnos.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;


import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ReservaService {

    @Autowired
    private ReservaRepository reservaRepository;
    @Autowired
    private GoogleCalendarService googleCalendarService;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private SalaRepository salaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    // ✅ Zona horaria fija del negocio (evita problemas si el server corre en otra zona)
    private static final ZoneId ZONE_ID = ZoneId.of("America/Argentina/Buenos_Aires");

    /**
     * Regla de negocio elegida:
     * - La reserva NO puede comenzar "hoy".
     * - Además, debe tener al menos X horas de anticipación desde "ahora".
     *
     * Mínimo permitido:
     *   max( inicioDelDiaDeManana , ahora + X horas )
     */
    private static final long MIN_ANTICIPACION_HORAS = 1;

    public static final DateTimeFormatter FECHA_HORA_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");


    @Transactional
    public Reserva saveReserva(Long clienteId, Long salaId,
                               LocalDateTime fechaInicio,
                               LocalDateTime fechaFinal,
                               Reserva.TipoPago tipoPago,
                               BigDecimal monto) {

        // ✅ Validación principal solicitada:
        // No permitir crear reservas en el pasado ni "para hoy".
        // Se exige que el inicio sea, como mínimo, desde el día siguiente,
        // respetando además un margen de anticipación (MIN_ANTICIPACION_HORAS).
        validarFechaInicioParaCreacion(fechaInicio);

        // Coherencia básica: la fecha final debe ser estrictamente posterior
        if (!fechaFinal.isAfter(fechaInicio)) {
            throw new IllegalArgumentException("La fecha final debe ser posterior a la inicial");
        }


        LocalDate hoy = LocalDate.now();

        // 1) No permitir reservas en días anteriores a hoy
        if (fechaInicio.toLocalDate().isBefore(hoy)) {
            throw new IllegalArgumentException("No se puede crear una reserva en una fecha anterior a hoy");
        }

        // 2) Coherencia básica de rango
        if (fechaFinal.isBefore(fechaInicio)) {
            throw new IllegalArgumentException("La fecha final debe ser posterior a la inicial");
        }

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        // No permitimos reservar una sala eliminada.
        Sala sala = salaRepository.findByIdAndEliminadaFalse(salaId)
                .orElseThrow(() -> new RuntimeException("Sala no encontrada o fue eliminada"));


        List<Reserva> conflictingReservas =
                reservaRepository.findConflictingReservas(salaId, fechaInicio, fechaFinal);
        if (!conflictingReservas.isEmpty()) {
            throw new TiempoDeReservaOcupadoException("El turno se superpone con otro existente");
        }
        Reserva reserva = new Reserva();
        reserva.setCliente(cliente);
        reserva.setSala(sala);
        reserva.setFechaInicio(fechaInicio);
        reserva.setFechaFinal(fechaFinal);
        reserva.setTipoPago(tipoPago);
        reserva.setEstado(Reserva.Estado.ACTIVO);
        reserva.setMonto(monto);

        reservaRepository.save(reserva);

        // Crear evento en Google Calendar
        try {
            String titulo = "Reserva de " + reserva.getCliente().getNombre();
            String descripcion = "Sala: " + reserva.getSala().getNumero() + "\nEmailCliente: " + reserva.getCliente().getEmail();


            Event evento = googleCalendarService.crearEventoConReserva(titulo, descripcion, reserva.getFechaInicio(), reserva.getFechaFinal());

            // Guardar el ID del evento en la reserva
            reserva.setGoogleEventId(evento.getId());

            // Guardar de nuevo en la base con el ID de Google
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException("Error al crear el evento en Google Calendar: " + e.getMessage(), e);
        }
        return reservaRepository.save(reserva);
    }

    @Transactional
    public void cancelarReservaPorCliente(Long reservaId, Long clienteId) {

        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ReservaNotFoundException("Reserva no encontrada"));

        if (!reserva.getCliente().getId().equals(clienteId)) {
            throw new AccesoProhibidoException("No tienes permiso para cancelar esta reserva");
        }

        if (reserva.getEstado() != Reserva.Estado.ACTIVO) {
            throw new ReservaNoCancelableException("Solo se pueden cancelar reservas activas");
        }

        if (reserva.getFechaInicio().isBefore(LocalDateTime.now())) {
            throw new ReservaNoCancelableException("No se puede cancelar una reserva que ya ha comenzado");
        }

        reserva.setEstado(Reserva.Estado.CANCELADO);
        googleCalendarService.eliminarEvento(reserva.getGoogleEventId());
        reservaRepository.save(reserva);
    }

    @Transactional
    public void cancelarReservaPorEmpleado(Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ReservaNotFoundException("Reserva no encontrada"));

        if (reserva.getEstado() != Reserva.Estado.ACTIVO) {
            throw new ReservaNoCancelableException("Solo se pueden cancelar reservas activas");
        }

        if (reserva.getFechaInicio().isBefore(LocalDateTime.now())) {
            throw new ReservaNoCancelableException("No se puede cancelar una reserva que ya ha comenzado");
        }
        reserva.setEstado(Reserva.Estado.CANCELADO);
        googleCalendarService.eliminarEvento(reserva.getGoogleEventId());
        reservaRepository.save(reserva);
    }


    @Transactional
    public void modificar(Reserva reserva) {
        if (!reservaRepository.existsById(reserva.getId())) {
            throw new EntityNotFoundException("La reserva no existe");
        }

        // ✅ Misma regla que al crear: evita bypass por modificación
        validarFechaInicioParaCreacion(reserva.getFechaInicio());

        if (!reserva.getFechaFinal().isAfter(reserva.getFechaInicio())) {
            throw new IllegalArgumentException("La fecha final debe ser posterior a la inicial");
        }

        LocalDate hoy = LocalDate.now();

        if (reserva.getFechaInicio().toLocalDate().isBefore(hoy)) {
            throw new IllegalArgumentException("No se puede mover la reserva a una fecha anterior a hoy");
        }

        if (reserva.getFechaFinal().isBefore(reserva.getFechaInicio())) {
            throw new IllegalArgumentException("La fecha final debe ser posterior a la inicial");
        }

        List<Reserva> conflictingReservas =
                reservaRepository.findConflictingReservas(
                                reserva.getSala().getId(),
                                reserva.getFechaInicio(),
                                reserva.getFechaFinal()
                        ).stream()
                        .filter(r -> !r.getId().equals(reserva.getId()))
                        .toList();

        if (!conflictingReservas.isEmpty()) {
            throw new TiempoDeReservaOcupadoException("El turno se superpone con otro existente");
        }

        reservaRepository.save(reserva);

        // Modificar evento en Google Calendar
        String titulo = "Reserva de " + reserva.getCliente().getNombre();
        String descripcion = "Sala: " + reserva.getSala().getNumero() + "\nEmailCliente: " + reserva.getCliente().getEmail();

        googleCalendarService.modificarEvento(
                reserva.getGoogleEventId(),
                titulo,
                descripcion,
                reserva.getFechaInicio(),
                reserva.getFechaFinal()
        );
    }


    public List<Reserva> listarTodas() {
        return reservaRepository.findAll();
    }

    public List<Reserva> listarPorUsuarioActual() {
        Usuario usuario = obtenerUsuarioActual();
        return reservaRepository.findByClienteId(usuario.getId());
    }

    public Reserva buscarPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada con ID: " + id));
    }

    public void eliminar(Long id) throws Exception {

        if (reservaRepository.existsById(id)) {
            Optional<Reserva> reserva = reservaRepository.findById(id);
            googleCalendarService.eliminarEvento(reserva.get().getGoogleEventId());
            reservaRepository.deleteById(id);
        } else {
            throw new IllegalArgumentException("La reserva no existe");
        }
    }


    private Usuario obtenerUsuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado por email: " + email));
    }

    public List<Reserva> findActiveByClienteId(Long clienteId) {
        return reservaRepository.findByClienteIdAndEstado(clienteId, Reserva.Estado.ACTIVO);
    }

    public Optional<Reserva> findById(Long id) {
        return reservaRepository.findById(id);
    }

    public List<Reserva> findAll() {
        return reservaRepository.findAll();
    }


    public List<Reserva> findAllActivas() {
        finalizarReservasVencidas(); // ✅ evita “ACTIVAS viejas”
        return reservaRepository.findByEstado(Reserva.Estado.ACTIVO);
    }

    public List<Reserva> findByEstado(Reserva.Estado estado) {
        return reservaRepository.findByEstado(estado);
    }


    public boolean existsActiveReservaForSala(Long salaId) {
        // Chequeo eficiente: evita traer una lista completa desde DB.
        // Incluye PENDIENTE_CONFIRMACION_PAGO porque esas reservas también bloquean la sala.
        return reservaRepository.existsBySalaIdAndEstado(salaId, Reserva.Estado.ACTIVO)
                || reservaRepository.existsBySalaIdAndEstado(salaId, Reserva.Estado.PENDIENTE_CONFIRMACION_PAGO);
    }


    public void updateReserva(Reserva reserva) {
        reservaRepository.save(reserva);
    }

    /**
     * Historial de un cliente: reservas pasadas o no activas,
     * ordenadas de la más nueva a la más vieja.
     * Excluye ACTIVO y PENDIENTE_CONFIRMACION_PAGO (esas van en "activas").
     */
    public List<Reserva> obtenerHistorialCliente(Long idCliente) {
        LocalDateTime ahora = LocalDateTime.now();

        return reservaRepository.findByClienteId(idCliente)
                .stream()
                .filter(reserva ->
                        reserva.getFechaFinal().isBefore(ahora)
                                || (reserva.getEstado() != Reserva.Estado.ACTIVO
                                    && reserva.getEstado() != Reserva.Estado.PENDIENTE_CONFIRMACION_PAGO)
                )
                .sorted(Comparator.comparing(Reserva::getFechaInicio).reversed())
                .toList();
    }

    /**
     * Historial general: todas las reservas pasadas o no activas,
     * ordenadas de la más nueva a la más vieja.
     * Excluye ACTIVO y PENDIENTE_CONFIRMACION_PAGO (esas van en "activas").
     */
    public List<Reserva> obtenerHistorialGeneral() {
        LocalDateTime ahora = LocalDateTime.now();

        return reservaRepository.findAll()
                .stream()
                .filter(reserva ->
                        reserva.getFechaFinal().isBefore(ahora)
                                || (reserva.getEstado() != Reserva.Estado.ACTIVO
                                    && reserva.getEstado() != Reserva.Estado.PENDIENTE_CONFIRMACION_PAGO)
                )
                .sorted(Comparator.comparing(Reserva::getFechaInicio).reversed())
                .toList();
    }

    /**
     * Valida que la fecha de inicio cumpla la regla de negocio para CREAR reservas.
     *
     * Regla:
     *  - No se permite reservar "hoy" (aunque sea más tarde).
     *  - Y se exige un mínimo de anticipación (MIN_ANTICIPACION_HORAS) desde el momento actual.
     */
    private void validarFechaInicioParaCreacion(LocalDateTime fechaInicio) {
        // Hora actual (del negocio) usando zona explícita
        LocalDateTime ahora = LocalDateTime.now(ZONE_ID);

        // Inicio del día siguiente (00:00 de mañana)
        LocalDateTime inicioManana = LocalDate.now(ZONE_ID).plusDays(1).atStartOfDay();

        // Anticipación mínima desde "ahora" (ej: ahora + 2 horas)
        LocalDateTime minimoPorAnticipacion = ahora.plusHours(MIN_ANTICIPACION_HORAS);

        // Mínimo definitivo: el mayor entre "mañana 00:00" y "ahora + X horas"
        LocalDateTime minimoPermitido = inicioManana.isAfter(minimoPorAnticipacion)
                ? inicioManana
                : minimoPorAnticipacion;

        if (fechaInicio.isBefore(minimoPermitido)) {
            // Mensaje pensado para que el frontend pueda mostrarlo tal cual
            throw new IllegalArgumentException(
                    "La reserva debe comenzar a partir de " + minimoPermitido.format(FECHA_HORA_FMT)
                            + " (hora local)."
            );
        }
    }

    /**
     * Marca como FINALIZADO todas las reservas que:
     * - siguen en estado ACTIVO
     * - pero su fechaFinal ya pasó (<= ahora)
     *
     * Se ejecuta:
     * - automáticamente por un scheduler (cada 60s), y
     * - también antes de listar/buscar reservas (para consistencia inmediata).
     */
    @Transactional
    public int finalizarReservasVencidas() {
        LocalDateTime ahora = LocalDateTime.now(ZONE_ID);

        // 1) Traer vencidas activas (necesitamos datos para calendar)
        List<Reserva> vencidas = reservaRepository
                .findByEstadoAndFechaFinalLessThanEqual(Reserva.Estado.ACTIVO, ahora);

        if (vencidas.isEmpty()) return 0;

        // 2) Pasarlas a FINALIZADO en DB
        vencidas.forEach(r -> r.setEstado(Reserva.Estado.FINALIZADO));
        reservaRepository.saveAll(vencidas);

        // 3) Mejor esfuerzo: marcar evento en Calendar (no rompe si falla uno)
        for (Reserva r : vencidas) {
            String eventId = r.getGoogleEventId();
            if (eventId == null || eventId.isBlank()) continue;

            try {
                googleCalendarService.marcarEventoFinalizado(eventId, r, ahora);
            } catch (Exception e) {
                log.warn("No se pudo marcar FINALIZADO en Google Calendar. reservaId={} eventId={} error={}",
                        r.getId(), eventId, e.getMessage());
            }
        }

        return vencidas.size();
    }


    /**
     * Reservas que se muestran en el calendario interno de la web.
     *
     * - Incluye: ACTIVO + PENDIENTE_CONFIRMACION_PAGO + FINALIZADO
     * - Excluye: CANCELADO (porque ya lo eliminás del Google Calendar y no querés mostrarlo aquí)
     */
    public List<Reserva> findAllActivasYFinalizadas() {
        // ✅ Antes de devolver, aseguramos que todo lo vencido esté FINALIZADO
        finalizarReservasVencidas();

        return reservaRepository.findByEstadoIn(List.of(
                Reserva.Estado.ACTIVO,
                Reserva.Estado.PENDIENTE_CONFIRMACION_PAGO,
                Reserva.Estado.FINALIZADO
        ));
    }




}
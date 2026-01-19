// GoogleCalendarService.java
package com.utn.gestion_de_turnos.API_Calendar.Service;

import com.google.api.client.util.DateTime;
import com.google.api.client.util.Value;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import com.utn.gestion_de_turnos.exception.GoogleCalendarException;
import com.utn.gestion_de_turnos.model.Reserva;
import com.utn.gestion_de_turnos.model.Usuario;
import com.utn.gestion_de_turnos.repository.ReservaRepository;
import com.utn.gestion_de_turnos.repository.UsuarioRepository;
import com.utn.gestion_de_turnos.API_Calendar.Factory.GoogleCalendarClientFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static com.utn.gestion_de_turnos.service.ReservaService.FECHA_HORA_FMT;

@Service
public class GoogleCalendarService {

    private final static String CALENDAR_ID = "f552183e1de85afe0892395dd0e5dab6c914f97427d62e4b1e64b82c1705871f@group.calendar.google.com";

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GoogleCalendarClientFactory calendarClientFactory;

    @Value("${google.calendar.color.finalizado:2}")
    private String colorFinalizadoId; // ✅ configurable (default 2)

    private Calendar getCalendar() throws IOException, GeneralSecurityException {
        return calendarClientFactory.getCalendarService();
    }

    public Event crearEventoConReserva(String resumen, String descripcion, LocalDateTime inicio, LocalDateTime fin)
            throws IOException, GeneralSecurityException {
        Calendar calendar = getCalendar();

        Event event = new Event()
                .setSummary(resumen)
                .setDescription(descripcion)
                .setStart(new EventDateTime()
                        .setDateTime(new DateTime(Date.from(inicio.atZone(ZoneId.systemDefault()).toInstant())))
                        .setTimeZone("America/Argentina/Buenos_Aires"))
                .setEnd(new EventDateTime()
                        .setDateTime(new DateTime(Date.from(fin.atZone(ZoneId.systemDefault()).toInstant())))
                        .setTimeZone("America/Argentina/Buenos_Aires"));

        return calendar.events().insert(CALENDAR_ID, event).execute();
    }

    public void eliminarEvento(String idEvento) {
        try {
            Optional<Reserva> reservaOpt = reservaRepository.findByGoogleEventId(idEvento);

            if (reservaOpt.isEmpty()) {
                throw new IllegalArgumentException("No existe una reserva asociada a este evento");
            }

            Reserva reserva = reservaOpt.get();
            Usuario actual = getUsuarioActual();

            if (actual.getRol().equals("CLIENTE") && !reserva.getCliente().getId().equals(actual.getId())) {
                throw new SecurityException("No puedes eliminar reservas de otros usuarios");
            }

            Calendar calendar = getCalendar();
            calendar.events().delete(CALENDAR_ID, idEvento).execute();
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException("Error al eliminar el evento de Google Calendar", e);
        }
    }


    public void modificarEvento(String idEvento, String nuevoTitulo, String nuevaDescripcion, LocalDateTime inicio, LocalDateTime fin) {
        try {
            Calendar calendar = getCalendar();
            Event event = calendar.events().get(CALENDAR_ID, idEvento).execute();
            event.setSummary(nuevoTitulo);
            event.setDescription(nuevaDescripcion);
            event.setStart(new EventDateTime()
                    .setDateTime(new DateTime(Date.from(inicio.atZone(ZoneId.systemDefault()).toInstant())))
                    .setTimeZone("America/Argentina/Buenos_Aires"));
            event.setEnd(new EventDateTime()
                    .setDateTime(new DateTime(Date.from(fin.atZone(ZoneId.systemDefault()).toInstant())))
                    .setTimeZone("America/Argentina/Buenos_Aires"));

            Optional<Reserva> reservaOpt = reservaRepository.findByGoogleEventId(idEvento);
            if (reservaOpt.isPresent()) {
                Reserva reserva = reservaOpt.get();
                Usuario actual = getUsuarioActual();
                if (actual.getRol().equals("CLIENTE") && !reserva.getCliente().getId().equals(actual.getId())) {
                    throw new SecurityException("No puedes modificar reservas de otros usuarios");
                }
            }

            calendar.events().update(CALENDAR_ID, idEvento, event).execute();

        } catch (IOException | GeneralSecurityException e) {
            throw new GoogleCalendarException("Error al modificar el evento en Google Calendar" + e.getMessage());
        }
    }


    public List<Event> listarEventos() throws Exception {
        Calendar calendar = getCalendar();
        Events events = calendar.events().list("CALENDAR_ID").execute();
        List<Event> items = events.getItems();
        Usuario actual = getUsuarioActual();

        if (actual.getRol().equals("CLIENTE")) {
            return items.stream()
                    .filter(event -> reservaRepository.findByGoogleEventId(event.getId())
                            .map(r -> r.getCliente().getId().equals(actual.getId()))
                            .orElse(false))
                    .collect(Collectors.toList());
        }
        return items;
    }

    public List<Event> filtrarEventos(Long idSala, Long idCliente, LocalDate fecha) throws Exception {
        Calendar calendar = getCalendar();
        List<Reserva> reservasFiltradas = reservaRepository.findAll().stream()
                .filter(reserva -> (idSala == null || (reserva.getSala() != null && reserva.getSala().getId().equals(idSala)))
                        && (idCliente == null || reserva.getCliente().getId().equals(idCliente))
                        && (fecha == null || reserva.getFechaInicio().toLocalDate().equals(fecha)))
                .collect(Collectors.toList());

        List<String> idsEventos = reservasFiltradas.stream()
                .map(Reserva::getGoogleEventId)
                .collect(Collectors.toList());

        Events allEvents = calendar.events().list("CALENDAR_ID").execute();

        return allEvents.getItems().stream()
                .filter(event -> idsEventos.contains(event.getId()))
                .collect(Collectors.toList());
    }


    public Event obtenerEventoPorId(String idEvento) throws Exception {
        Calendar calendar = getCalendar();
        Event event = calendar.events().get("CALENDAR_ID", idEvento).execute();

        Usuario actual = getUsuarioActual();
        Optional<Reserva> reservaOpt = reservaRepository.findByGoogleEventId(idEvento);

        if (actual.getRol().equals("CLIENTE") && reservaOpt.isPresent()) {
            Reserva reserva = reservaOpt.get();
            if (!reserva.getCliente().getId().equals(actual.getId())) {
                throw new SecurityException("No puedes acceder a eventos de otros usuarios");
            }
        }
        return event;
    }

    private Usuario getUsuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }



    public void marcarEventoFinalizado(String idEvento, Reserva reserva, LocalDateTime finalizadoEn) {
        try {
            Calendar calendar = getCalendar();
            Event event = calendar.events().get(CALENDAR_ID, idEvento).execute();

            // ✅ 1) Cambiar color
            event.setColorId(colorFinalizadoId);

            // ✅ 2) Marcar el titulo para que se vea rapido en Calendar
            String summary = event.getSummary() == null ? "" : event.getSummary();
            if (!summary.startsWith("✅ FINALIZADO - ")) {
                event.setSummary("✅ FINALIZADO - " + summary);
            }

            // ✅ 3) Agregar bloque en la descripcion
            String desc = event.getDescription() == null ? "" : event.getDescription();
            if (!desc.contains("Estado: FINALIZADO")) {
                desc += "\n\n---\nEstado: FINALIZADO\nReserva ID: " + reserva.getId()
                        + "\nFinalizado: " + finalizadoEn.format(FECHA_HORA_FMT) + "\n";
            }
            event.setDescription(desc);

            // ✅ 4) Metadata interna (no siempre visible en UI, pero útil)
            Event.ExtendedProperties props = event.getExtendedProperties();
            if (props == null) props = new Event.ExtendedProperties();

            Map<String, String> priv = props.getPrivate();
            if (priv == null) priv = new HashMap<>();

            priv.put("reservaId", String.valueOf(reserva.getId()));
            priv.put("estado", "FINALIZADO");
            priv.put("finalizadoEn", finalizadoEn.toString());

            props.setPrivate(priv);
            event.setExtendedProperties(props);

            calendar.events().update(CALENDAR_ID, idEvento, event).execute();

        } catch (IOException | GeneralSecurityException e) {
            throw new GoogleCalendarException("Error al marcar evento FINALIZADO en Google Calendar: " + e.getMessage());
        }
    }


}

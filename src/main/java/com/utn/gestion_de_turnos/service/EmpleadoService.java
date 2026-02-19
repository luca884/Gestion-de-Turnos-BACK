package com.utn.gestion_de_turnos.service;

import com.fasterxml.jackson.databind.deser.CreatorProperty;
import com.utn.gestion_de_turnos.model.Cliente;
import com.utn.gestion_de_turnos.model.Empleado;
import com.utn.gestion_de_turnos.model.Usuario;
import com.utn.gestion_de_turnos.repository.ClienteRepository;
import com.utn.gestion_de_turnos.repository.EmpleadoRepository;
import com.utn.gestion_de_turnos.repository.UsuarioRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
public class EmpleadoService {

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

//    public Empleado save(Empleado empleado) {
//        if (empleado == null) {
//            throw new IllegalArgumentException("Empleado no puede ser nulo");
//        }
//        if (empleado.getId() == null) {
//            empleado.setContrasena(passwordEncoder.encode(empleado.getContrasena()));
//        } else {
//            Empleado existente = empleadoRepository.findById(empleado.getId())
//                    .orElseThrow(() -> new IllegalArgumentException("Empleado no encontrado con ID: " + empleado.getId()));
//            empleado.setContrasena(existente.getContrasena());
//        }
//        return empleadoRepository.save(empleado);
//    }
public Empleado save(Empleado empleado) {
    if (empleado == null) {
        throw new IllegalArgumentException("Empleado no puede ser nulo");
    }

    System.out.println("=== EmpleadoService.save() ===");
    System.out.println("ID recibido: " + empleado.getId());
    System.out.println("Email recibido: " + empleado.getEmail());
    System.out.println("Contraseña recibida (tal cual llega): " + empleado.getContrasena());

    // ✅ CASO 1: crear nuevo empleado
    if (empleado.getId() == null) {
        if (empleado.getContrasena() == null || empleado.getContrasena().isBlank()) {
            throw new IllegalArgumentException("La contraseña no puede ser nula o vacía al registrar un empleado nuevo");
        }

        // Chequeo DNI único
        if (empleado.getDni() != null && !empleado.getDni().isBlank()) {
            java.util.Optional<Usuario> existePorDni = usuarioRepository.findByDni(empleado.getDni());
            if (existePorDni.isPresent()) {
                throw new IllegalArgumentException("DNI ya registrado");
            }
        }

        // encriptamos la pass en texto plano
        empleado.setContrasena(passwordEncoder.encode(empleado.getContrasena()));
        return empleadoRepository.save(empleado);
    }

    // ✅ CASO 2: actualizar empleado existente
    Empleado existente = empleadoRepository.findById(empleado.getId())
            .orElseThrow(() -> new IllegalArgumentException("Empleado no encontrado con ID: " + empleado.getId()));

    System.out.println("Hash actual en BD: " + existente.getContrasena());

    // 2.1 Chequeo DNI único (solo si cambió)
    String dniNuevo = empleado.getDni();
    String dniActual = existente.getDni();
    if (dniNuevo != null && !dniNuevo.isBlank() && !dniNuevo.equals(dniActual)) {
        java.util.Optional<Usuario> existePorDni = usuarioRepository.findByDni(dniNuevo);
        if (existePorDni.isPresent() && !existePorDni.get().getId().equals(empleado.getId())) {
            throw new IllegalArgumentException("DNI ya registrado");
        }
    }

    // 2.2 actualizar datos básicos
    existente.setNombre(empleado.getNombre());
    existente.setApellido(empleado.getApellido());
    existente.setDni(empleado.getDni());
    existente.setTelefono(empleado.getTelefono());
    existente.setEmail(empleado.getEmail());
    existente.setLegajo(empleado.getLegajo());
    existente.setRol(empleado.getRol());

    // 2.2 lógica de contraseña
    String contrasenaEnviada = empleado.getContrasena();   // lo que mandó el front
    String hashActual = existente.getContrasena();         // lo que hay en la BD

    if (contrasenaEnviada == null || contrasenaEnviada.isBlank()) {
        // NO tocamos la contraseña
    } else if (contrasenaEnviada.equals(hashActual)) {
        // llegó el mismo hash → no cambiamos nada
    } else {
        // asumimos que lo que viene es una NUEVA pass en texto plano
        String nuevoHash = passwordEncoder.encode(contrasenaEnviada);
        existente.setContrasena(nuevoHash);
    }

    return empleadoRepository.save(existente);
}


    public Optional<Empleado> findById(Long id) {
        return empleadoRepository.findById(id);
    }

    public List<Empleado> findAll() {
        return empleadoRepository.findAll();
    }

    public void deleteById(Long id) {
        if (!empleadoRepository.existsById(id)) {
            throw new IllegalArgumentException("Empleado con ID " + id + " no existe");
        }
        empleadoRepository.deleteById(id);
    }

    public Empleado findByEmail(String email) {
        Empleado empleado = empleadoRepository.findByEmail(email);

        if (empleado == null) {
            System.out.println("Empleado con email " + email + " no encontrado.");
        } else {
            System.out.println("Empleado encontrado: " + empleado.getEmail());
        }

        return empleado;
    }

    public List<Cliente> findAllClientes() {
        return clienteRepository.findAll();
    }


    public Empleado login(String email, String contrasena) {
        Empleado empleado = empleadoRepository.findByEmail(email);

        if (empleado == null) {
            System.out.println("Email no encontrado: " + email);
            return null;
        }

        if (passwordEncoder.matches(contrasena, empleado.getContrasena())) {
            System.out.println("Login exitoso para: " + empleado.getEmail());
            return empleado;
        }

        System.out.println("Contraseña incorrecta para el email: " + email);
        return null;
    }
}

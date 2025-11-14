package com.utn.gestion_de_turnos.service;

import com.utn.gestion_de_turnos.model.Cliente;
import com.utn.gestion_de_turnos.repository.ClienteRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;


@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

//    public Cliente save(Cliente cliente) {
//        if (cliente == null) {
//            throw new IllegalArgumentException("Cliente no puede ser nulo");
//        }
//
//        if (cliente.getId() == null) {
//            cliente.setContrasena(passwordEncoder.encode(cliente.getContrasena()));
//        } else {
//            Cliente existente = clienteRepository.findById(cliente.getId())
//                    .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado con ID: " + cliente.getId()));
//            cliente.setContrasena(existente.getContrasena());
//        }
//
//        return clienteRepository.save(cliente);
//    }

    public Cliente save(Cliente cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException("Cliente no puede ser nulo");
        }

        // ✅ CASO 1: crear nuevo cliente
        if (cliente.getId() == null) {
            if (cliente.getContrasena() == null || cliente.getContrasena().isBlank()) {
                throw new IllegalArgumentException("La contraseña no puede ser nula o vacía al registrar un cliente nuevo");
            }

            // Encriptamos la contraseña en texto plano
            cliente.setContrasena(passwordEncoder.encode(cliente.getContrasena()));
            return clienteRepository.save(cliente);
        }

        // ✅ CASO 2: actualizar cliente existente
        Cliente existente = clienteRepository.findById(cliente.getId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado con ID: " + cliente.getId()));

        // 2.1 Actualizamos los datos básicos
        existente.setNombre(cliente.getNombre());
        existente.setApellido(cliente.getApellido());
        existente.setDni(cliente.getDni());
        existente.setTelefono(cliente.getTelefono());
        existente.setEmail(cliente.getEmail());
        existente.setRol(cliente.getRol());

        // 2.2 Lógica de contraseña
        String contrasenaEnviada = cliente.getContrasena();      // lo que mandó el front
        String hashActual = existente.getContrasena();           // lo que hay en la BD

        if (contrasenaEnviada == null || contrasenaEnviada.isBlank()) {
            // Si llega null o vacío, NO tocamos la contraseña
            // (podrías lanzar error si preferís forzar siempre algo, pero con tu front no hace falta)
        } else if (contrasenaEnviada.equals(hashActual)) {
            // Si el front envía el mismo hash que ya teníamos (caso "no toqué el campo pero se mandó igual"),
            // no cambiamos nada
        } else {
            // Si es distinto al hash actual, asumimos que es una NUEVA contraseña en texto plano
            String nuevoHash = passwordEncoder.encode(contrasenaEnviada);
            existente.setContrasena(nuevoHash);
        }

        // 2.3 Guardamos y devolvemos el cliente actualizado
        return clienteRepository.save(existente);
    }


    public Optional<Cliente> findById(Long id) {
        return clienteRepository.findById(id);
    }

    public List<Cliente> findAll() {
        return clienteRepository.findAll();
    }

    public void deleteById(Long id) {
        if (!clienteRepository.existsById(id)) {
            throw new IllegalArgumentException("Cliente con ID " + id + " no existe");
        }
        clienteRepository.deleteById(id);
    }

    public Optional<Cliente> findByEmail(String email) {
        Cliente cliente = clienteRepository.findByEmail(email);
        if (cliente != null) {
            System.out.println("Cliente encontrado: " + cliente.getEmail());
            return Optional.of(cliente);
        } else {
            System.out.println("Cliente con email " + email + " no encontrado.");
            return Optional.empty();
        }
    }

    public Cliente login(String email, String contrasena) {
        Cliente cliente = clienteRepository.findByEmail(email);

        if (cliente == null) {
            System.out.println("Email no encontrado: " + email);
            return null;
        }

        if (passwordEncoder.matches(contrasena, cliente.getContrasena())) {
            System.out.println("Login exitoso para: " + cliente.getEmail());
            return cliente;
        }

        System.out.println("Contraseña incorrecta para el email: " + email);
        return null;
    }
}
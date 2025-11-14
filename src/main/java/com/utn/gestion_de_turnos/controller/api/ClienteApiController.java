package com.utn.gestion_de_turnos.controller.api;

import com.utn.gestion_de_turnos.model.Cliente;
import com.utn.gestion_de_turnos.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

// Eso es un controlador REST para manejar las operaciones CRUD de la entidad Cliente.

@RestController
@RequestMapping("/api/cliente")
@Tag(name = "Cliente", description = "Operaciones relacionadas con los clientes")
public class ClienteApiController {
    @Autowired
    private ClienteService clienteService;

    @PostMapping
    @Operation(
            summary = "Crear un nuevo cliente",
            description = "Crea un cliente nuevo en el sistema"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente creado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Cliente.class),
                            examples = @ExampleObject(value = "{ \"nombre\": \"Juan\", \"apellido\": \"Pérez\", \"email\": \"juan@example.com\", \"dni\": \"12345678\", \"telefono\": \"1122334455\" }")
                    ))
    })
    public Cliente createCliente(@RequestBody Cliente cliente) {
        return clienteService.save(cliente);
    }

    @GetMapping("/{id}")

    @Operation(
            summary = "Obtener cliente por ID",
            description = "Devuelve los datos del cliente según su ID"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado",
                    content = @Content(schema = @Schema(implementation = Cliente.class))),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    public ResponseEntity<Cliente> getClienteById(@PathVariable Long id) {
        return clienteService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Listar todos los clientes",
            description = "Devuelve una lista de todos los clientes"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de clientes",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Cliente.class)
                    ))
    })
    @GetMapping("/all")
    public List<Cliente> getAllClientes() {
        return clienteService.findAll();
    }


    @Operation(
            summary = "Eliminar cliente por ID",
            description = "Elimina un cliente del sistema por su ID"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente eliminado"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    @DeleteMapping("/{id}")
    public void deleteCliente(@PathVariable Long id) {
        clienteService.deleteById(id);
    }

    @PutMapping("/update")
    @Operation(
            summary = "Actualizar un cliente existente",
            description = "Actualiza los datos de un cliente. Si la contraseña enviada es distinta al hash actual, se encripta como una nueva contraseña."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente actualizado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Cliente.class)
                    )),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    public ResponseEntity<?> updateCliente(@RequestBody Cliente cliente) {
        try {
            Cliente actualizado = clienteService.save(cliente);
            return ResponseEntity.ok(actualizado);
        } catch (IllegalArgumentException e) {
            // casos como "cliente no encontrado" o "contraseña inválida"
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace(); // para ver el error real en la consola
            return ResponseEntity.internalServerError().body("Error al actualizar el cliente");
        }
    }

}

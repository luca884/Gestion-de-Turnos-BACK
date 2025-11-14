package com.utn.gestion_de_turnos.controller.api;
import com.utn.gestion_de_turnos.model.Cliente;
import com.utn.gestion_de_turnos.model.Empleado;
import com.utn.gestion_de_turnos.service.EmpleadoService;
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

@Tag(name = "Empleado", description = "Operaciones relacionadas con empleados")
@RestController
@RequestMapping("/api/empleados")
public class EmpleadoApiController {

    @Autowired
    private EmpleadoService empleadoService;


    @Operation(
            summary = "Crear un nuevo empleado",
            description = "Registra un nuevo empleado en el sistema"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empleado creado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Empleado.class),
                            examples = @ExampleObject(value = "{ \"nombre\": \"Lucía\", \"apellido\": \"Fernández\", \"email\": \"lucia@example.com\", \"dni\": \"45678901\", \"telefono\": \"1166554433\" }")
                    ))
    })
    @PostMapping
    public Empleado createEmpleado(@RequestBody Empleado empleado) {
        return empleadoService.save(empleado);
    }

    @Operation(
            summary = "Obtener un empleado por ID",
            description = "Devuelve la información de un empleado específico por ID"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empleado encontrado",
                    content = @Content(schema = @Schema(implementation = Empleado.class))),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado")
    })
    @GetMapping("/{id}")
    public Optional<Empleado> getEmpleadoById(@PathVariable Long id) {
        return empleadoService.findById(id);
    }

    @Operation(
            summary = "Obtener todos los empleados",
            description = "Devuelve una lista de todos los empleados registrados"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de empleados obtenida",
                    content = @Content(schema = @Schema(implementation = Empleado.class)))
    })
    @GetMapping
    public List<Empleado> getAllEmpleados() {
        return empleadoService.findAll();
    }


    @Operation(
            summary = "Eliminar un empleado por ID",
            description = "Elimina un empleado existente por su ID"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empleado eliminado"),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado")
    })
    @DeleteMapping("/{id}")
    public void deleteEmpleado(@PathVariable Long id) {
        empleadoService.deleteById(id);
    }

    // 🔁 UPDATE (pensado para PUT /api/empleado/{id})
    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar un empleado existente",
            description = "Actualiza los datos de un empleado. Si la contraseña enviada es distinta al hash actual, se encripta como una nueva contraseña."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Empleado actualizado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Empleado.class)
                    )),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado")
    })
    public ResponseEntity<?> updateEmpleado(@PathVariable Long id, @RequestBody Empleado empleado) {
        try {
            // nos aseguramos de que el ID del body coincida con el de la URL
            empleado.setId(id);
            Empleado actualizado = empleadoService.save(empleado);
            return ResponseEntity.ok(actualizado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error al actualizar el empleado");
        }
    }

}

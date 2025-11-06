package com.utn.gestion_de_turnos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserInfoResponseDTO {
    private Long id;
    private String email;
    private String role;

}

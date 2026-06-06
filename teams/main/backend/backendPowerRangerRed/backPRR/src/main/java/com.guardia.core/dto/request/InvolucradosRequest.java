package com.guardia.core.dto.request;

import com.guardia.core.model.enums.TipoRol;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class InvolucradosRequest {

    @NotBlank
    private String nombre;

    private String apellido;

    @NotBlank
    private String cedula;

    private String telefono;

    private String nacionalidad;

    private String direccion;

    private String fotografiaURL;

    private TipoRol rol;

    private String relacionConHecho;
}

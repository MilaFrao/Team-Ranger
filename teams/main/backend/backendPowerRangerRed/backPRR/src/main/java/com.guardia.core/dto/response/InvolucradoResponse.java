package com.guardia.core.dto.response;

import com.guardia.core.model.enums.TipoRol;

public record InvolucradoResponse(
        Long id,
        String nombre,
        String identificacion,
        String numeroTelefono,
        String nacionalidad,
        String direccion,
        TipoRol rol,
        String relacionConHecho
) {}
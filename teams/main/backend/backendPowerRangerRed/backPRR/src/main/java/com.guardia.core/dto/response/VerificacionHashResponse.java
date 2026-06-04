package com.guardia.core.dto.response;

public record VerificacionHashResponse(
        Long expedienteId,
        String folio,
        boolean integro,
        String mensaje,
        String hashAlmacenado,
        String hashRecalculado
) {}
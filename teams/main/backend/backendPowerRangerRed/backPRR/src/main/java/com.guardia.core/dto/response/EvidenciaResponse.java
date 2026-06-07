package com.guardia.core.dto.response;
import java.time.LocalDateTime;

public record EvidenciaResponse(
        Long id,
        String numeroItem,
        String tipo,
        String descripcion,
        Long escenaId,
        String hashIntegridad,
        LocalDateTime timestampRegistro,
        String investigadorNombre
) {}

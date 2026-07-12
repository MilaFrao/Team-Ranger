package com.guardia.core.repository.dto;

import java.time.LocalDateTime;

public record ExpedienteSimilitudRow(
        Long expedienteId, String folio, String tipoDelito, String subtipoDelito,
        LocalDateTime fechaHecho, String investigadorAsignado, double similitudPorcentual
) {
}
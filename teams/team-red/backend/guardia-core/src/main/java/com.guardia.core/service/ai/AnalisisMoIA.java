package com.guardia.core.service.ai;

public record AnalisisMoIA(
        String caracteristicasComunes, String posibleFirma,
        String consistenciaHorarioZona, String resumenGenerado, Double nivelConfianza
) {
}
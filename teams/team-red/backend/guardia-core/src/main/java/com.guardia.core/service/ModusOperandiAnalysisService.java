package com.guardia.core.service;

import com.guardia.core.dto.response.PropuestaModusOperandiResponse;
import com.guardia.core.model.Expediente;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ModusOperandiAnalysisService {
    CompletableFuture<Void> analizarExpedienteAsync(Long expedienteId);
    void analizarPatrones(List<Expediente> expedientes);
    double compararExpedientes(Expediente a, Expediente b);
    void calcularNivelConfianza();
    void generarAlerta(String criterio);
    void agregarPatron(String patron);
    PropuestaModusOperandiResponse obtenerPropuestaActual(Long expedienteId);
    List<PropuestaModusOperandiResponse> obtenerHistorial(Long expedienteId);
}
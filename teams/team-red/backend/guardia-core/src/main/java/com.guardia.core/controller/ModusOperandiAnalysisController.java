package com.guardia.core.controller;
package com.guardia.core.dto.request;

import com.guardia.core.dto.request.GenerarAlertaManualRequest;
import com.guardia.core.dto.response.PropuestaModusOperandiResponse;
import com.guardia.core.exception.ApiResponse;
import com.guardia.core.exception.BusinessException;
import com.guardia.core.exception.ResourceNotFoundException;
import com.guardia.core.model.Expediente;
import com.guardia.core.repository.ExpedienteRepository;
import com.guardia.core.service.ModusOperandiAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

public record GenerarAlertaManualRequest(
        @NotBlank(message = "Debe indicar el criterio o patrón a buscar.")
        String criterio
) {
}

@RestController
@RequestMapping("/api/v1/analisis-mo")
@RequiredArgsConstructor
public class ModusOperandiAnalysisController {

    private final ModusOperandiAnalysisService analysisService;
    private final ExpedienteRepository expedienteRepository;

    @GetMapping("/expedientes/{expedienteId}")
    public ResponseEntity<ApiResponse<PropuestaModusOperandiResponse>> obtenerPropuestaActual(
            @PathVariable Long expedienteId) {
        return ResponseEntity.ok(ApiResponse.ok(analysisService.obtenerPropuestaActual(expedienteId)));
    }

    @GetMapping("/expedientes/{expedienteId}/historial")
    public ResponseEntity<ApiResponse<List<PropuestaModusOperandiResponse>>> obtenerHistorial(
            @PathVariable Long expedienteId) {
        return ResponseEntity.ok(ApiResponse.ok(analysisService.obtenerHistorial(expedienteId)));
    }

    @PostMapping("/expedientes/{expedienteId}/analizar")
    public ResponseEntity<ApiResponse<Void>> reanalizar(@PathVariable Long expedienteId) {
        analysisService.analizarExpedienteAsync(expedienteId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.ok("Análisis de Modus Operandi encolado.", null));
    }

    @GetMapping("/comparar")
    public ResponseEntity<ApiResponse<Map<String, Object>>> comparar(
            @RequestParam Long expedienteAId, @RequestParam Long expedienteBId) {
        Expediente a = expedienteRepository.findById(expedienteAId)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente", expedienteAId));
        Expediente b = expedienteRepository.findById(expedienteBId)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente", expedienteBId));
        double similitud = analysisService.compararExpedientes(a, b);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "expedienteAId", expedienteAId, "expedienteBId", expedienteBId,
                "similitudPorcentual", similitud)));
    }

    @PostMapping("/alertas/generar")
    public ResponseEntity<ApiResponse<Void>> generarAlertaManual(
            @Valid @RequestBody GenerarAlertaManualRequest request) {
        analysisService.generarAlerta(request.criterio());
        return ResponseEntity.ok(ApiResponse.ok(
                "Búsqueda ejecutada. Si hubo suficientes coincidencias, la alerta quedará en /api/v1/alertas-patron.", null));
    }

    @PostMapping("/catalogo/patrones")
    public ResponseEntity<ApiResponse<Void>> agregarPatron(@RequestBody Map<String, String> body) {
        String patron = body.get("patron");
        if (patron == null || patron.isBlank()) throw new BusinessException("Debe indicar el campo 'patron'.");
        analysisService.agregarPatron(patron);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Patrón registrado en el catálogo.", null));
    }

    @PostMapping("/mantenimiento/recalcular-confianza")
    public ResponseEntity<ApiResponse<Void>> recalcularConfianza() {
        analysisService.calcularNivelConfianza();
        return ResponseEntity.ok(ApiResponse.ok("Recalculo de confianza ejecutado.", null));
    }
}
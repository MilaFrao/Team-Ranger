package com.guardia.core.service;

import com.guardia.core.dto.request.EscenaRequest;
import com.guardia.core.dto.response.EscenaResponse;
import com.guardia.core.dto.response.EscenaChecklistResponse;

import java.util.List;

public interface EscenaService {
    EscenaResponse crear(EscenaRequest request);
    EscenaResponse obtenerPorId(Long id);
    List<EscenaResponse> obtenerTodos();
    List<EscenaResponse> obtenerPorExpediente(Long expedienteId);
    List<EscenaResponse> obtenerPorInvestigador(Long usuarioId);
    List<EscenaChecklistResponse> obtenerChecklist(Long id);
    void eliminar(Long id);
    EscenaResponse iniciarChecklist(Long id);
    EscenaResponse cerrar(Long id);
    EscenaResponse bloquearEdicion(Long id);
    boolean validarSecuencia(Long id);
    EscenaResponse avanzarPaso(Long id);
}

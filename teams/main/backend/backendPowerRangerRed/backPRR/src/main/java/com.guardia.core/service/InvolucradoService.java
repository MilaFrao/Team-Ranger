package com.guardia.core.service;

import com.guardia.core.dto.response.InvolucradoResponse;
import com.guardia.core.model.enums.TipoRol;

import java.util.List;

public interface InvolucradoService {
    InvolucradoResponse obtenerPorId(Long id);
    List<InvolucradoResponse> obtenerTodos();
    List<InvolucradoResponse> obtenerPorExpediente(Long expedienteId);
    List<InvolucradoResponse> obtenerPorRol(TipoRol rol);
    void eliminar(Long id);
}
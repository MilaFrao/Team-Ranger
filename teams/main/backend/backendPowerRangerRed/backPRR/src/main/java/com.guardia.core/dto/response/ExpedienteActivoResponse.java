package com.guardia.core.dto.response;

import com.guardia.core.model.SubtipoDelito;
import com.guardia.core.model.TipoDelito;
import com.guardia.core.model.enums.EstadoExpediente;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpedienteActivoResponse {
    private Long id;
    private String folioCOPP;
    private TipoDelito tipoDelito;
    private SubtipoDelito subtipoDelito;
    private LocalDateTime fechaHecho;
    private LocalDateTime fechaCreacion;
    private String investigadorAsignado;
    private EstadoExpediente estatus;
    private boolean tieneAlertaPatron;
    private String municipio;
    private String sector;
}
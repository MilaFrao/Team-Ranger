package com.guardia.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteSimilarRef {

    @Column(name = "expediente_relacionado_id")
    private Long expedienteId;

    @Column(name = "expediente_relacionado_folio", length = 60)
    private String folio;

    @Column(name = "similitud_coseno")
    private Double similitud;
}
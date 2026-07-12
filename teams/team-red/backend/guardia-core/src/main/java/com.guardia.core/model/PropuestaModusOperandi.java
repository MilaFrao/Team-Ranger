package com.guardia.core.model;

import com.guardia.core.model.enums.EstadoPropuestaMO;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "propuestas_modus_operandi")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropuestaModusOperandi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediente_id", nullable = false)
    private Expediente expediente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modus_operandi_id")
    private ModusOperandi modusOperandi;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false)
    private Boolean vigente;

    @Column(name = "caracteristicas_comunes", columnDefinition = "TEXT")
    private String caracteristicasComunes;

    @Column(name = "posible_firma", columnDefinition = "TEXT")
    private String posibleFirma;

    @Column(name = "consistencia_horario_zona", columnDefinition = "TEXT")
    private String consistenciaHorarioZona;

    @Column(name = "resumen_generado", columnDefinition = "TEXT")
    private String resumenGenerado;

    @Column(name = "nivel_confianza", precision = 5, scale = 2)
    private BigDecimal nivelConfianza;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoPropuestaMO estado;

    @ElementCollection
    @CollectionTable(name = "propuesta_mo_expediente_similar", joinColumns = @JoinColumn(name = "propuesta_mo_id"))
    @Builder.Default
    private List<ExpedienteSimilarRef> expedientesSimilares = new ArrayList<>();

    @Column(name = "modelo_embedding", length = 80)
    private String modeloEmbedding;

    @Column(name = "modelo_chat", length = 80)
    private String modeloChat;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    @Column(name = "revisado_por_experto", nullable = false)
    @Builder.Default
    private Boolean revisadoPorExperto = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analista_revisor_id")
    private Usuario analistaRevisor;

    @Column(name = "justificacion_revision", columnDefinition = "TEXT")
    private String justificacionRevision;

    @Column(name = "fecha_revision")
    private LocalDateTime fechaRevision;

    public boolean esSinCoincidencias() {
        return estado == EstadoPropuestaMO.SIN_COINCIDENCIAS;
    }

    public void marcarHistorica() {
        this.vigente = false;
    }

    public boolean bloqueadaParaAutomatizacion() {
        return Boolean.TRUE.equals(this.revisadoPorExperto);
    }
}
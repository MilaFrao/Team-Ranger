package com.guardia.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guardia.core.dto.response.ExpedienteSimilarResponse;
import com.guardia.core.dto.response.PropuestaModusOperandiResponse;
import com.guardia.core.dto.response.UsuarioResponse;
import com.guardia.core.event.ExpedienteRegistradoEvent;
import com.guardia.core.exception.BusinessException;
import com.guardia.core.exception.ResourceNotFoundException;
import com.guardia.core.model.AlertaPatron;
import com.guardia.core.model.Expediente;
import com.guardia.core.model.ExpedienteSimilarRef;
import com.guardia.core.model.ModusOperandi;
import com.guardia.core.model.PropuestaModusOperandi;
import com.guardia.core.model.enums.EstadoAlerta;
import com.guardia.core.model.enums.EstadoPropuestaMO;
import com.guardia.core.repository.AlertaPatronRepository;
import com.guardia.core.repository.ExpedienteEmbeddingRepository;
import com.guardia.core.repository.ExpedienteRepository;
import com.guardia.core.repository.ModusOperandiRepository;
import com.guardia.core.repository.PropuestaModusOperandiRepository;
import com.guardia.core.repository.dto.ExpedienteSimilitudRow;
import com.guardia.core.service.ai.AnalisisMoIA;
import com.guardia.core.service.ai.PromptTemplates;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModusOperandiAnalysisServiceImpl implements ModusOperandiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ModusOperandiAnalysisServiceImpl.class);

    private final ExpedienteRepository expedienteRepository;
    private final ExpedienteEmbeddingRepository expedienteEmbeddingRepository;
    private final PropuestaModusOperandiRepository propuestaRepository;
    private final AlertaPatronRepository alertaPatronRepository;
    private final ModusOperandiRepository modusOperandiRepository;
    private final EmbeddingModel embeddingModel;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    // Auto-referencia perezosa al proxy de Spring: necesaria para que
    // analizarPatrones() invoque analizarExpedienteAsync() de forma
    // realmente asíncrona (llamar "this.metodo()" dentro de la misma clase
    // NO pasa por el proxy y @Async se ignoraría).
    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private ModusOperandiAnalysisService self;

    @Value("${zac.mo.top-k:5}")
    private int topK;

    @Value("${zac.mo.umbral-similitud-candidato:70.0}")
    private double umbralSimilitudCandidato;

    @Value("${zac.mo.umbral-confianza-alerta:75.0}")
    private double umbralConfianzaAlerta;

    @Value("${zac.mo.minimo-similares-alerta:2}")
    private int minimoSimilaresAlerta;

    @Value("${spring.ai.openai.embedding.model:text-embedding-3-small}")
    private String nombreModeloEmbedding;

    @Value("${spring.ai.openai.chat.model:gpt-4o}")
    private String nombreModeloChat;

    @EventListener
    @Async("moTaskExecutor")
    @Transactional
    public void onExpedienteRegistrado(ExpedienteRegistradoEvent event) {
        Expediente expediente = event.getExpediente();
        if (expediente == null || expediente.getId() == null) return;
        try {
            ejecutarAnalisis(expediente.getId());
        } catch (Exception ex) {
            log.error("No se pudo completar el análisis de MO disparado por evento para el expediente {}: {}",
                    expediente.getId(), ex.getMessage(), ex);
        }
    }

    @Override
    @Async("moTaskExecutor")
    @Transactional
    public CompletableFuture<Void> analizarExpedienteAsync(Long expedienteId) {
        try {
            ejecutarAnalisis(expedienteId);
        } catch (Exception ex) {
            log.error("No se pudo completar el análisis de MO para el expediente {}: {}",
                    expedienteId, ex.getMessage(), ex);
        }
        return CompletableFuture.completedFuture(null);
    }

    private void ejecutarAnalisis(Long expedienteId) {
        Expediente expediente = expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente", expedienteId));

        Optional<PropuestaModusOperandi> vigenteActual =
                propuestaRepository.findByExpedienteIdAndVigenteTrue(expedienteId);

        if (vigenteActual.isPresent() && vigenteActual.get().bloqueadaParaAutomatizacion()) {
            log.info("Expediente {}: propuesta ya revisada por experto; no se sobrescribe.", expedienteId);
            return;
        }

        String descripcion = expediente.getDescripcionHecho();
        if (descripcion == null || descripcion.isBlank()) {
            log.warn("Expediente {}: sin descripción del hecho, se omite el análisis de MO.", expedienteId);
            return;
        }

        float[] embedding = embeddingModel.embed(descripcion);
        expedienteEmbeddingRepository.actualizarEmbedding(expedienteId, embedding);

        List<ExpedienteSimilitudRow> similares = expedienteEmbeddingRepository.buscarSimilares(
                embedding, expedienteId, topK, umbralSimilitudCandidato);

        vigenteActual.ifPresent(p -> { p.marcarHistorica(); propuestaRepository.save(p); });
        int siguienteVersion = vigenteActual.map(p -> p.getVersion() + 1).orElse(1);

        if (similares.isEmpty()) {
            PropuestaModusOperandi propuestaVacia = PropuestaModusOperandi.builder()
                    .expediente(expediente).version(siguienteVersion).vigente(true)
                    .estado(EstadoPropuestaMO.SIN_COINCIDENCIAS)
                    .resumenGenerado("MO sin coincidencias previas.")
                    .nivelConfianza(BigDecimal.ZERO).expedientesSimilares(List.of())
                    .modeloEmbedding(nombreModeloEmbedding).fechaGeneracion(LocalDateTime.now())
                    .revisadoPorExperto(false).build();
            propuestaRepository.save(propuestaVacia);
            return;
        }

        AnalisisMoIA analisis = consultarLlm(expediente, similares);
        BigDecimal confianza = normalizarConfianza(analisis.nivelConfianza());

        List<ExpedienteSimilarRef> refs = similares.stream()
                .map(s -> ExpedienteSimilarRef.builder()
                        .expedienteId(s.expedienteId()).folio(s.folio())
                        .similitud(s.similitudPorcentual()).build())
                .toList();

        PropuestaModusOperandi propuesta = PropuestaModusOperandi.builder()
                .expediente(expediente).version(siguienteVersion).vigente(true)
                .estado(EstadoPropuestaMO.PENDIENTE)
                .caracteristicasComunes(analisis.caracteristicasComunes())
                .posibleFirma(analisis.posibleFirma())
                .consistenciaHorarioZona(analisis.consistenciaHorarioZona())
                .resumenGenerado(analisis.resumenGenerado())
                .nivelConfianza(confianza).expedientesSimilares(refs)
                .modeloEmbedding(nombreModeloEmbedding).modeloChat(nombreModeloChat)
                .fechaGeneracion(LocalDateTime.now()).revisadoPorExperto(false).build();
        propuesta = propuestaRepository.save(propuesta);

        if (similares.size() >= minimoSimilaresAlerta && confianza.doubleValue() >= umbralConfianzaAlerta) {
            crearAlertaSiNoExisteDuplicada(expediente, similares, analisis.resumenGenerado(), confianza, propuesta);
        }
    }

    private AnalisisMoIA consultarLlm(Expediente expediente, List<ExpedienteSimilitudRow> similares) {
        Prompt prompt = new Prompt(List.of(
                new SystemMessage(PromptTemplates.SYSTEM_ANALISIS_MO),
                new UserMessage(PromptTemplates.userAnalisisMo(expediente, similares))));
        ChatResponse response = chatModel.call(prompt);
        return parsearRespuestaJson(response.getResult().getOutput().getText());
    }

    private AnalisisMoIA parsearRespuestaJson(String contenido) {
        String limpio = contenido == null ? "" : contenido.trim();
        if (limpio.startsWith("```")) {
            limpio = limpio.replaceFirst("^```(json)?", "").trim();
            if (limpio.endsWith("```")) limpio = limpio.substring(0, limpio.length() - 3).trim();
        }
        try {
            return objectMapper.readValue(limpio, AnalisisMoIA.class);
        } catch (Exception e) {
            log.error("No se pudo interpretar como JSON la respuesta del modelo: {}", contenido, e);
            return new AnalisisMoIA("No se pudo estructurar el análisis automáticamente.",
                    "No determinado", "No determinado",
                    contenido == null || contenido.isBlank() ? "Sin respuesta del modelo." : contenido, 0.0);
        }
    }

    private BigDecimal normalizarConfianza(Double valor) {
        double v = valor == null ? 0.0 : valor;
        if (v < 0) v = 0; if (v > 100) v = 100;
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public void analizarPatrones(List<Expediente> expedientes) {
        if (expedientes == null || expedientes.isEmpty()) return;
        for (Expediente e : expedientes) {
            if (e != null && e.getId() != null) self.analizarExpedienteAsync(e.getId());
        }
    }

    @Override
    public double compararExpedientes(Expediente a, Expediente b) {
        if (a == null || a.getId() == null || b == null || b.getId() == null) {
            throw new BusinessException("Ambos expedientes deben existir y tener id para poder compararse.");
        }
        return expedienteEmbeddingRepository.compararPorId(a.getId(), b.getId()).orElse(0.0);
    }

    @Override
    @Transactional
    public void calcularNivelConfianza() {
        List<PropuestaModusOperandi> pendientes =
                propuestaRepository.findByEstadoAndVigenteTrue(EstadoPropuestaMO.PENDIENTE);
        for (PropuestaModusOperandi propuesta : pendientes) {
            if (propuesta.getExpediente() == null || propuesta.getExpediente().getId() == null) continue;
            List<Long> relacionadosIds = propuesta.getExpedientesSimilares().stream()
                    .map(ExpedienteSimilarRef::getExpedienteId).toList();
            if (relacionadosIds.isEmpty()) continue;
            Optional<Double> promedio = expedienteEmbeddingRepository.promedioSimilitudContra(
                    propuesta.getExpediente().getId(), relacionadosIds);
            promedio.ifPresent(valor -> {
                propuesta.setNivelConfianza(BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP));
                propuestaRepository.save(propuesta);
            });
        }
    }

    @Override
    @Transactional
    public void generarAlerta(String criterio) {
        if (criterio == null || criterio.isBlank()) {
            throw new BusinessException("Debe indicar un criterio o patrón para buscar.");
        }
        float[] embeddingCriterio = embeddingModel.embed(criterio);
        List<ExpedienteSimilitudRow> coincidencias =
                expedienteEmbeddingRepository.buscarPorEmbeddingLibre(embeddingCriterio, topK);
        if (coincidencias.size() < minimoSimilaresAlerta + 1) return;

        ExpedienteSimilitudRow origenRow = coincidencias.get(0);
        Expediente origen = expedienteRepository.findById(origenRow.expedienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Expediente", origenRow.expedienteId()));
        List<ExpedienteSimilitudRow> relacionados = coincidencias.subList(1, coincidencias.size());
        double confianzaPromedio = coincidencias.stream()
                .mapToDouble(ExpedienteSimilitudRow::similitudPorcentual).average().orElse(0.0);
        String resumen = "Coincidencia manual por criterio: \"%s\". Se encontraron %d expedientes relacionados."
                .formatted(criterio, coincidencias.size());
        crearAlertaSiNoExisteDuplicada(origen, relacionados, resumen,
                BigDecimal.valueOf(confianzaPromedio).setScale(2, RoundingMode.HALF_UP), null);
    }

    @Override
    @Transactional
    public void agregarPatron(String patron) {
        if (patron == null || patron.isBlank()) throw new BusinessException("El patrón no puede estar vacío.");
        String normalizado = patron.trim();
        boolean existe = !modusOperandiRepository.findByPatronDetectadoContainingIgnoreCase(normalizado).isEmpty();
        if (existe) return;
        ModusOperandi nuevo = new ModusOperandi();
        nuevo.setPatronDetectado(normalizado);
        nuevo.setDescripcionAnalitica("Patrón registrado manualmente en el catálogo por un Analista Criminal.");
        nuevo.setExpedientes(new ArrayList<>());
        modusOperandiRepository.save(nuevo);
    }

    @Override
    @Transactional(readOnly = true)
    public PropuestaModusOperandiResponse obtenerPropuestaActual(Long expedienteId) {
        PropuestaModusOperandi propuesta = propuestaRepository.findByExpedienteIdAndVigenteTrue(expedienteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Propuesta de Modus Operandi para el expediente", expedienteId));
        return toResponse(propuesta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropuestaModusOperandiResponse> obtenerHistorial(Long expedienteId) {
        return propuestaRepository.findByExpedienteIdOrderByVersionDesc(expedienteId)
                .stream().map(this::toResponse).toList();
    }

    private void crearAlertaSiNoExisteDuplicada(Expediente origen, List<ExpedienteSimilitudRow> relacionados,
                                                String resumenPatron, BigDecimal nivelConfianza,
                                                PropuestaModusOperandi propuestaOrigen) {
        List<Long> idsConjunto = new ArrayList<>();
        idsConjunto.add(origen.getId());
        relacionados.forEach(r -> idsConjunto.add(r.expedienteId()));
        Collections.sort(idsConjunto);
        String hash = hashConjunto(idsConjunto);

        Optional<AlertaPatron> existente = alertaPatronRepository.findFirstByHashConjuntoAndFechaGeneracionAfter(
                hash, LocalDateTime.now().minusHours(24));
        if (existente.isPresent()) return;

        List<ExpedienteSimilarRef> refs = new ArrayList<>();
        refs.add(ExpedienteSimilarRef.builder()
                .expedienteId(origen.getId()).folio(origen.getFolio()).similitud(100.0).build());
        relacionados.forEach(r -> refs.add(ExpedienteSimilarRef.builder()
                .expedienteId(r.expedienteId()).folio(r.folio()).similitud(r.similitudPorcentual()).build()));

        AlertaPatron alerta = AlertaPatron.builder()
                .expedienteOrigen(origen).propuestaModusOperandi(propuestaOrigen)
                .expedientesRelacionados(refs).resumenPatron(resumenPatron)
                .nivelConfianza(nivelConfianza).hashConjunto(hash)
                .estado(EstadoAlerta.PENDIENTE).fechaGeneracion(LocalDateTime.now()).build();
        alertaPatronRepository.save(alerta);
    }

    private String hashConjunto(List<Long> idsOrdenados) {
        String base = idsOrdenados.stream().map(String::valueOf).collect(Collectors.joining("-"));
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(base.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM.", e);
        }
    }

    private PropuestaModusOperandiResponse toResponse(PropuestaModusOperandi p) {
        UsuarioResponse analista = p.getAnalistaRevisor() == null ? null : new UsuarioResponse(
                p.getAnalistaRevisor().getId(), p.getAnalistaRevisor().getNombre(),
                p.getAnalistaRevisor().getIdentificacion(), p.getAnalistaRevisor().getCorreo());
        List<ExpedienteSimilarResponse> similares = p.getExpedientesSimilares().stream()
                .map(s -> new ExpedienteSimilarResponse(s.getExpedienteId(), s.getFolio(), s.getSimilitud()))
                .toList();
        return new PropuestaModusOperandiResponse(p.getId(),
                p.getExpediente() != null ? p.getExpediente().getId() : null,
                p.getExpediente() != null ? p.getExpediente().getFolio() : null,
                p.getVersion(), p.getVigente(), p.getCaracteristicasComunes(), p.getPosibleFirma(),
                p.getConsistenciaHorarioZona(), p.getResumenGenerado(), p.getNivelConfianza(), p.getEstado(),
                similares, p.getRevisadoPorExperto(), analista, p.getJustificacionRevision(),
                p.getFechaGeneracion(), p.getFechaRevision(),
                p.getModusOperandi() != null ? p.getModusOperandi().getId() : null);
    }
}
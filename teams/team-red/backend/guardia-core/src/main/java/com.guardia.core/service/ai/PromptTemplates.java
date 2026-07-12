package com.guardia.core.service.ai;

import com.guardia.core.model.Expediente;
import com.guardia.core.repository.dto.ExpedienteSimilitudRow;

import java.util.List;
import java.util.stream.Collectors;

public final class PromptTemplates {

    private PromptTemplates() {
    }

    public static final String SYSTEM_ANALISIS_MO = """
            Eres un analista criminal experto que apoya a investigadores policiales
            en la identificación de patrones de Modus Operandi (MO) entre expedientes
            delictivos. Tu tarea es comparar la descripción de un delito nuevo contra
            un conjunto de expedientes previos recuperados por similitud semántica, y
            determinar qué tienen en común.

            Responde EXCLUSIVAMENTE con un objeto JSON válido, sin texto adicional,
            sin bloques de código Markdown, con exactamente esta forma:
            {
              "caracteristicasComunes": "string, características objetivas compartidas",
              "posibleFirma": "string, firma conductual reconocible, o \\"No se identifica una firma clara\\" si no aplica",
              "consistenciaHorarioZona": "string, consistencia de horario y/o zona geográfica",
              "resumenGenerado": "string, explicación completa en lenguaje natural",
              "nivelConfianza": number entre 0 y 100
            }

            Si la evidencia es débil, dilo explícitamente y asigna un nivelConfianza bajo.
            No inventes hechos que no estén en las descripciones provistas.
            """;

    public static String userAnalisisMo(Expediente nuevo, List<ExpedienteSimilitudRow> similares) {
        String bloqueSimilares = similares.stream()
                .map(s -> "- Folio %s | %s%s | %s%% similitud | fecha: %s"
                        .formatted(s.folio(),
                                s.tipoDelito() != null ? s.tipoDelito() : "N/D",
                                s.subtipoDelito() != null ? " / " + s.subtipoDelito() : "",
                                String.format("%.1f", s.similitudPorcentual()),
                                s.fechaHecho() != null ? s.fechaHecho().toLocalDate() : "N/D"))
                .collect(Collectors.joining("\n"));

        return """
                Delito nuevo (folio %s):
                "%s"

                Expedientes previos recuperados por similitud vectorial de embeddings:
                %s

                Analiza qué tienen en común el delito nuevo y los expedientes previos
                listados arriba, y responde con el JSON indicado en las instrucciones.
                """.formatted(nuevo.getFolio(), safe(nuevo.getDescripcionHecho()),
                bloqueSimilares.isBlank() ? "(ninguno)" : bloqueSimilares);
    }

    private static String safe(String texto) {
        return texto == null || texto.isBlank() ? "(sin descripción registrada)" : texto;
    }
}
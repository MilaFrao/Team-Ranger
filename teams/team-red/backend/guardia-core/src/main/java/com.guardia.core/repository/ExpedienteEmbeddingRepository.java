package com.guardia.core.repository;

import com.guardia.core.repository.dto.ExpedienteSimilitudRow;
import com.guardia.core.service.util.VectorUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ExpedienteEmbeddingRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public void actualizarEmbedding(Long expedienteId, float[] embedding) {
        String literal = VectorUtils.toPgVectorLiteral(embedding);
        Query query = entityManager.createNativeQuery(
                "UPDATE expedientes SET embedding = CAST(:vector AS vector) WHERE id = :id");
        query.setParameter("vector", literal);
        query.setParameter("id", expedienteId);
        query.executeUpdate();
    }

    public boolean tieneEmbedding(Long expedienteId) {
        Query query = entityManager.createNativeQuery(
                "SELECT (embedding IS NOT NULL) FROM expedientes WHERE id = :id");
        query.setParameter("id", expedienteId);
        return Boolean.TRUE.equals(query.getSingleResult());
    }

    @SuppressWarnings("unchecked")
    public Optional<Double> compararPorId(Long expedienteIdA, Long expedienteIdB) {
        Query query = entityManager.createNativeQuery(
                "SELECT (1 - (a.embedding <=> b.embedding)) " +
                        "FROM expedientes a, expedientes b " +
                        "WHERE a.id = :idA AND b.id = :idB " +
                        "AND a.embedding IS NOT NULL AND b.embedding IS NOT NULL");
        query.setParameter("idA", expedienteIdA);
        query.setParameter("idB", expedienteIdB);
        List<Object> rows = query.getResultList();
        if (rows.isEmpty() || rows.get(0) == null) return Optional.empty();
        double cosenoSimilitud = ((Number) rows.get(0)).doubleValue();
        return Optional.of(Math.max(0, Math.min(100, cosenoSimilitud * 100.0)));
    }

    @SuppressWarnings("unchecked")
    public List<ExpedienteSimilitudRow> buscarSimilares(float[] embeddingConsulta, Long excluirExpedienteId,
                                                        int limite, double umbralSimilitudPorcentual) {
        String literal = VectorUtils.toPgVectorLiteral(embeddingConsulta);
        String sql = "SELECT e.id, e.folio, td.nombre AS tipo_delito, sd.nombre AS subtipo_delito, " +
                "e.fecha_hecho, u.nombre AS investigador, " +
                "(1 - (e.embedding <=> CAST(:vector AS vector))) AS similitud " +
                "FROM expedientes e " +
                "LEFT JOIN tipo_delito td ON td.id = e.tipo_delito_id " +
                "LEFT JOIN subtipo_delito sd ON sd.id = e.subtipo_delito_id " +
                "LEFT JOIN usuario u ON u.id = e.creado_por_id " +
                "WHERE e.embedding IS NOT NULL " +
                (excluirExpedienteId != null ? "AND e.id <> :excluirId " : "") +
                "AND (1 - (e.embedding <=> CAST(:vector AS vector))) >= :umbral " +
                "ORDER BY e.embedding <=> CAST(:vector AS vector) ASC " +
                "LIMIT :limite";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("vector", literal);
        query.setParameter("umbral", umbralSimilitudPorcentual / 100.0);
        query.setParameter("limite", limite);
        if (excluirExpedienteId != null) query.setParameter("excluirId", excluirExpedienteId);

        return mapear(query.getResultList());
    }

    public List<ExpedienteSimilitudRow> buscarPorEmbeddingLibre(float[] embeddingConsulta, int limite) {
        return buscarSimilares(embeddingConsulta, null, limite, 0.0);
    }

    @SuppressWarnings("unchecked")
    public Optional<Double> promedioSimilitudContra(Long expedienteId, List<Long> relacionadosIds) {
        if (relacionadosIds == null || relacionadosIds.isEmpty()) return Optional.empty();
        Query query = entityManager.createNativeQuery(
                "SELECT AVG(1 - (e.embedding <=> r.embedding)) " +
                        "FROM expedientes e, expedientes r " +
                        "WHERE e.id = :id AND r.id IN (:relacionados) " +
                        "AND e.embedding IS NOT NULL AND r.embedding IS NOT NULL");
        query.setParameter("id", expedienteId);
        query.setParameter("relacionados", relacionadosIds);
        List<Object> rows = query.getResultList();
        if (rows.isEmpty() || rows.get(0) == null) return Optional.empty();
        double promedio = ((Number) rows.get(0)).doubleValue() * 100.0;
        return Optional.of(Math.max(0, Math.min(100, promedio)));
    }

    private List<ExpedienteSimilitudRow> mapear(List<Object[]> filas) {
        List<ExpedienteSimilitudRow> resultado = new ArrayList<>(filas.size());
        for (Object[] fila : filas) {
            Long id = ((Number) fila[0]).longValue();
            String folio = (String) fila[1];
            String tipoDelito = (String) fila[2];
            String subtipoDelito = (String) fila[3];
            LocalDateTime fechaHecho = fila[4] instanceof Timestamp ts ? ts.toLocalDateTime() : null;
            String investigador = (String) fila[5];
            double similitud = ((Number) fila[6]).doubleValue() * 100.0;
            resultado.add(new ExpedienteSimilitudRow(id, folio, tipoDelito, subtipoDelito, fechaHecho,
                    investigador != null ? investigador : "Sin asignar", Math.max(0, Math.min(100, similitud))));
        }
        return resultado;
    }
}
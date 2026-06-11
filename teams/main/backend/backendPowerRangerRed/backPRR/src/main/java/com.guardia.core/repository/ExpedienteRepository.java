package com.guardia.core.repository;

import com.guardia.core.model.Expediente;
import com.guardia.core.model.enums.EstadoExpediente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpedienteRepository extends JpaRepository<Expediente, Long> {
    Optional<Expediente> findByFolio(String folio);
    List<Expediente> findByCreadoPorId(Long creadoPorId);
   // List<Expediente> findByEstatus(List<EstadoExpediente> estadoExpediente);
    @Query("SELECT e FROM Expediente e WHERE e.estadoExpediente = :enumEstado")
    List<Expediente> findByEstatus(@Param("enumEstado") EstadoExpediente enumEstado);
}


package com.guardia.core.repository;

import com.guardia.core.model.PropuestaModusOperandi;
import com.guardia.core.model.enums.EstadoPropuestaMO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropuestaModusOperandiRepository extends JpaRepository<PropuestaModusOperandi, Long> {
    Optional<PropuestaModusOperandi> findByExpedienteIdAndVigenteTrue(Long expedienteId);
    List<PropuestaModusOperandi> findByExpedienteIdOrderByVersionDesc(Long expedienteId);
    List<PropuestaModusOperandi> findByEstadoAndVigenteTrue(EstadoPropuestaMO estado);
    List<PropuestaModusOperandi> findByVigenteTrueAndRevisadoPorExpertoFalse();
}
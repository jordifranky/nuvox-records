package com.nuvoxrecords.backend.repository;

import com.nuvoxrecords.backend.entity.Demo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DemoRepository extends JpaRepository<Demo, Long> {

    Optional<Demo> findByCodigoExpediente(String codigoExpediente);

    List<Demo> findByEstadoCuratorial(String estadoCuratorial);
}

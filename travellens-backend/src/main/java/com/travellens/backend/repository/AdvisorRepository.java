package com.travellens.backend.repository;

import com.travellens.backend.model.Advisor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdvisorRepository extends JpaRepository<Advisor, Long> {
    List<Advisor> findByDestinationId(Long destinationId);
}

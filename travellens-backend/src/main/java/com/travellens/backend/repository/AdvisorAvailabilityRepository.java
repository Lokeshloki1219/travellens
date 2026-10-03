package com.travellens.backend.repository;

import com.travellens.backend.model.AdvisorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdvisorAvailabilityRepository extends JpaRepository<AdvisorAvailability, Long> {
    List<AdvisorAvailability> findByAdvisorIdAndBookedFalse(Long advisorId);
    List<AdvisorAvailability> findByAdvisorId(Long advisorId);
}

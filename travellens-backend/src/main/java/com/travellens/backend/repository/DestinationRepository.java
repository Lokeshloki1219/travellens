package com.travellens.backend.repository;

import com.travellens.backend.model.Destination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DestinationRepository extends JpaRepository<Destination, Long> {
    List<Destination> findByCategoryIgnoreCase(String category);
}

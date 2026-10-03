package com.travellens.backend.controller;

import com.travellens.backend.exception.ResourceNotFoundException;
import com.travellens.backend.model.Destination;
import com.travellens.backend.repository.DestinationRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/destinations")
public class DestinationController {

    private final DestinationRepository destinationRepository;

    public DestinationController(DestinationRepository destinationRepository) {
        this.destinationRepository = destinationRepository;
    }

    @GetMapping
    public List<Destination> getAll(@RequestParam(required = false) String category) {
        if (category != null && !category.isBlank()) {
            return destinationRepository.findByCategoryIgnoreCase(category);
        }
        return destinationRepository.findAll();
    }

    @GetMapping("/{id}")
    public Destination getOne(@PathVariable Long id) {
        return destinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Destination not found with id: " + id));
    }

    @PostMapping
    public Destination create(@RequestBody Destination destination) {
        return destinationRepository.save(destination);
    }

    @PutMapping("/{id}")
    public Destination update(@PathVariable Long id, @RequestBody Destination updated) {
        Destination existing = destinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Destination not found with id: " + id));

        existing.setName(updated.getName());
        existing.setCategory(updated.getCategory());
        existing.setDescription(updated.getDescription());
        existing.setBestTimeToVisit(updated.getBestTimeToVisit());
        existing.setBudgetEstimate(updated.getBudgetEstimate());
        existing.setRecommendedDuration(updated.getRecommendedDuration());
        existing.setMustVisitAttractions(updated.getMustVisitAttractions());
        existing.setTips(updated.getTips());
        existing.setThingsToAvoid(updated.getThingsToAvoid());
        existing.setImageUrl(updated.getImageUrl());

        return destinationRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        destinationRepository.deleteById(id);
    }
}

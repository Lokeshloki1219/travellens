package com.travellens.backend.controller;

import com.travellens.backend.exception.ResourceNotFoundException;
import com.travellens.backend.model.Advisor;
import com.travellens.backend.model.AdvisorAvailability;
import com.travellens.backend.model.Destination;
import com.travellens.backend.repository.AdvisorAvailabilityRepository;
import com.travellens.backend.repository.AdvisorRepository;
import com.travellens.backend.repository.DestinationRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/advisors")
public class AdvisorController {

    private final AdvisorRepository advisorRepository;
    private final AdvisorAvailabilityRepository availabilityRepository;
    private final DestinationRepository destinationRepository;

    public AdvisorController(AdvisorRepository advisorRepository,
                              AdvisorAvailabilityRepository availabilityRepository,
                              DestinationRepository destinationRepository) {
        this.advisorRepository = advisorRepository;
        this.availabilityRepository = availabilityRepository;
        this.destinationRepository = destinationRepository;
    }

    @GetMapping
    public List<Advisor> getAll(@RequestParam(required = false) Long destinationId) {
        if (destinationId != null) {
            return advisorRepository.findByDestinationId(destinationId);
        }
        return advisorRepository.findAll();
    }

    @GetMapping("/{id}")
    public Advisor getOne(@PathVariable Long id) {
        return advisorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Advisor not found with id: " + id));
    }

    @PostMapping
    public Advisor create(@RequestBody Advisor advisor) {
        if (advisor.getDestination() != null && advisor.getDestination().getId() != null) {
            Destination destination = destinationRepository.findById(advisor.getDestination().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Destination not found"));
            advisor.setDestination(destination);
        }
        return advisorRepository.save(advisor);
    }

    @GetMapping("/{id}/availability")
    public List<AdvisorAvailability> getAvailability(@PathVariable Long id,
                                                       @RequestParam(defaultValue = "false") boolean onlyOpen) {
        if (onlyOpen) {
            return availabilityRepository.findByAdvisorIdAndBookedFalse(id);
        }
        return availabilityRepository.findByAdvisorId(id);
    }

    @PostMapping("/{id}/availability")
    public AdvisorAvailability addAvailability(@PathVariable Long id, @RequestBody AdvisorAvailability slot) {
        Advisor advisor = advisorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Advisor not found with id: " + id));
        slot.setAdvisor(advisor);
        slot.setBooked(false);
        return availabilityRepository.save(slot);
    }
}

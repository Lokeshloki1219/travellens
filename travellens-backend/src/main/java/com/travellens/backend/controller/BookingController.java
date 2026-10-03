package com.travellens.backend.controller;

import com.travellens.backend.dto.BookingRequest;
import com.travellens.backend.exception.BadRequestException;
import com.travellens.backend.exception.ResourceNotFoundException;
import com.travellens.backend.model.*;
import com.travellens.backend.repository.AdvisorAvailabilityRepository;
import com.travellens.backend.repository.BookingRepository;
import com.travellens.backend.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingRepository bookingRepository;
    private final AdvisorAvailabilityRepository availabilityRepository;
    private final UserRepository userRepository;

    public BookingController(BookingRepository bookingRepository,
                              AdvisorAvailabilityRepository availabilityRepository,
                              UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.availabilityRepository = availabilityRepository;
        this.userRepository = userRepository;
    }

    private User currentUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @GetMapping("/me")
    public List<Booking> myBookings(Authentication authentication) {
        User user = currentUser(authentication);
        return bookingRepository.findByUserId(user.getId());
    }

    @PostMapping
    @Transactional
    public synchronized Booking createBooking(@Valid @RequestBody BookingRequest request, Authentication authentication) {
        User user = currentUser(authentication);

        AdvisorAvailability slot = availabilityRepository.findById(request.getAvailabilityId())
                .orElseThrow(() -> new ResourceNotFoundException("Availability slot not found"));

        if (slot.isBooked()) {
            throw new BadRequestException("This slot has already been booked. Please choose another one.");
        }

        slot.setBooked(true);
        availabilityRepository.save(slot);

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setAdvisor(slot.getAdvisor());
        booking.setAvailability(slot);
        booking.setStatus(BookingStatus.CONFIRMED);

        return bookingRepository.save(booking);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void cancelBooking(@PathVariable Long id, Authentication authentication) {
        User user = currentUser(authentication);

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You cannot cancel a booking that isn't yours");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        AdvisorAvailability slot = booking.getAvailability();
        if (slot != null) {
            slot.setBooked(false);
            availabilityRepository.save(slot);
        }
    }
}

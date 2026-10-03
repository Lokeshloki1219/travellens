package com.travellens.backend.dto;

import jakarta.validation.constraints.NotNull;

public class BookingRequest {

    @NotNull
    private Long availabilityId;

    public Long getAvailabilityId() {
        return availabilityId;
    }

    public void setAvailabilityId(Long availabilityId) {
        this.availabilityId = availabilityId;
    }
}

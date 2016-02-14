package com.quitevis.parkingmanager.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class VehicleRecord {
    private String vehicleId;
    private UUID ticketId;
    private LocalDateTime dateEntered;
    private LocalDateTime dateExited;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        VehicleRecord that = (VehicleRecord) o;

        return vehicleId.equals(that.vehicleId);
    }

    @Override
    public int hashCode() {
        return vehicleId.hashCode();
    }
}

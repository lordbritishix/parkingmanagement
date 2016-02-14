package com.quitevis.parkingmanager.server.logger;

import com.quitevis.parkingmanager.model.VehicleRecord;

/**
 * Logs the vehicles that are entering / exiting the parking lot
 */
public interface ParkingLogger {
    enum State {
        PARKED,
        EXITED_PARKING,
        UNABLE_TO_PARK
    }

    boolean log(VehicleRecord vehicleRecord, State state);
}

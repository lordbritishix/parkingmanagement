package com.quitevis.parkingmanager.server.logger;

import com.google.inject.Singleton;
import com.quitevis.parkingmanager.model.VehicleRecord;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Logs parking lot events using the slf4j backend
 */
@Slf4j
@Singleton
public class Slf4jParkingLogger implements  ParkingLogger {
    @Override
    public boolean log(VehicleRecord vehicleRecord, State state) {
        log.info("Date: {}; Event: {}; VehicleRecord: {}", LocalDateTime.now(ZoneOffset.UTC).toString(), state.name(), vehicleRecord.toString());
        return true;
    }
}

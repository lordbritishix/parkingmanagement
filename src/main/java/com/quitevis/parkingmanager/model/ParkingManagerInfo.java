package com.quitevis.parkingmanager.model;

import com.google.gson.Gson;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParkingManagerInfo {
    private final int maxCapacity;
    private final int currentCapacity;
    private final int capacityLeft;
    private final int entryCount;
    private final int exitCount;

    public static ParkingManagerInfo fromJson(String json) {
        Gson gson = new Gson();
        ParkingManagerInfo info = gson.fromJson(json, ParkingManagerInfo.class);
        return  info;
    }
}

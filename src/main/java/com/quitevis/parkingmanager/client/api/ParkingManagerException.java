package com.quitevis.parkingmanager.client.api;

import lombok.Data;

@Data
public class ParkingManagerException extends Exception {
    private int errorCode;

    public ParkingManagerException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ParkingManagerException(int errorCode, String message, Exception cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

}

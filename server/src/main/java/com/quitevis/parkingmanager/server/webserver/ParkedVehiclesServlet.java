package com.quitevis.parkingmanager.server.webserver;

import com.google.gson.JsonArray;
import com.google.inject.Inject;
import com.quitevis.parkingmanager.server.manager.ParkingManager;
import com.quitevis.parkingmanager.model.VehicleRecord;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

/**
 * Handles request for /rest/parked. It returns the vehicle id of the parked cars
 */
public class ParkedVehiclesServlet extends HttpServlet {
    private final ParkingManager parkingManager;

    @Inject
    public ParkedVehiclesServlet(ParkingManager parkingManager) {
        this.parkingManager = parkingManager;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        JsonArray array = new JsonArray();
        Set<VehicleRecord> vehicleRecordSet = parkingManager.getParkedVehicleIds();

        vehicleRecordSet.stream()
                .map(p -> p.getVehicleId())
                .forEach(p -> array.add(p));

        resp.setContentType("application/json;charset=utf-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().println(array.toString());
    }
}

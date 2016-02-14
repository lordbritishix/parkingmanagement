package com.quitevis.parkingmanager.server.webserver;

import com.google.gson.JsonObject;
import com.quitevis.parkingmanager.model.VehicleRecord;
import com.quitevis.parkingmanager.server.manager.ParkingManager;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
/**
 * Handles request for /rest/enter. This request is called whenever a vehicle needs to enter the parking lot
 * Parameters are:
 * vehicleId - unique id identifying a vehicle
 * gateId - the gate number where the car entered
 */
public class EnterServlet extends HttpServlet {
    private final ParkingManager parkingManager;

    public EnterServlet(ParkingManager parkingManager) {
        this.parkingManager = parkingManager;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=utf-8");
        JsonObject json = new JsonObject();

        try {
            String vehicleId = req.getParameter("vehicleId");
            int gateId = Integer.parseInt(req.getParameter("gateId"));
            CompletableFuture<UUID> future = parkingManager.enter(gateId, VehicleRecord.builder().vehicleId(vehicleId).build());
            UUID ticketId = future.get();
            json.addProperty("ticketId", ticketId.toString());
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            json.addProperty("errorCode", HttpServletResponse.SC_BAD_REQUEST);
            json.addProperty("message", e.getMessage());
            log.error("Unable to fulfill the /enter request", e);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } finally {
            resp.getWriter().println(json.toString());
        }
    }
}

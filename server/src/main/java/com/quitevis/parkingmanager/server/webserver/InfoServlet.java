package com.quitevis.parkingmanager.server.webserver;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.quitevis.parkingmanager.server.manager.ParkingManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Handles request for /rest/info. It returns information about the parking lot that the parking manager is managing
 */
public class InfoServlet extends HttpServlet {
    private final ParkingManager parkingManager;

    @Inject
    public InfoServlet(ParkingManager parkingManager) {
        this.parkingManager = parkingManager;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        JsonObject json = new JsonObject();
        json.addProperty("maxCapacity", parkingManager.getMaxCapacity());
        json.addProperty("currentCapacity", parkingManager.getCurrentCapacity());
        json.addProperty("capacityLeft", parkingManager.getCapacityLeft());
        json.addProperty("entryCount", parkingManager.getEntryCount());
        json.addProperty("exitCount", parkingManager.getExitCount());

        resp.setContentType("application/json;charset=utf-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().println(json.toString());
    }
}

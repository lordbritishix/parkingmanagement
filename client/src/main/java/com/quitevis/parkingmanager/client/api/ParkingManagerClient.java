package com.quitevis.parkingmanager.client.api;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.quitevis.parkingmanager.model.ParkingManagerInfo;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Talks to the Parking Manager API server using Jersey client
 */
public class ParkingManagerClient {
    private final String hostAndPort;

    @Inject
    public ParkingManagerClient(
            @Named("server.hostname") String hostname,
            @Named("server.port") int port) {
        this.hostAndPort = "http://" + hostname + ":" + port;
    }

    public ParkingManagerInfo getInfo() throws ParkingManagerException {
        Client client = Client.create();
        WebResource webResource = client.resource(hostAndPort + "/rest/info");

        ClientResponse response = webResource.accept("application/json")
                .get(ClientResponse.class);

        if (response.getStatus() != 200) {
            throw new ParkingManagerException(response.getStatus(), "Unable to get the parking manager info");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = new BufferedInputStream(response.getEntityInputStream())) {
            IOUtils.copy(is, baos);
            String json = new String(baos.toByteArray(), StandardCharsets.UTF_8);
            return ParkingManagerInfo.fromJson(json);
        } catch (IOException e) {
            throw new ParkingManagerException(500, "Unable to get the parking manager info", e);
        }
    }

    public UUID parkVehicle(String vehicleId, int gate) throws ParkingManagerException {
        Client client = Client.create();
        WebResource webResource = client.resource(hostAndPort + "/rest/enter?vehicleId=" + vehicleId + "&gateId=" + gate);

        ClientResponse response = webResource.accept("application/json")
                .post(ClientResponse.class);

        if (response.getStatus() != 200) {
            throw new ParkingManagerException(response.getStatus(), "Unable to park the vehicle");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = new BufferedInputStream(response.getEntityInputStream())) {
            IOUtils.copy(is, baos);
            String json = new String(baos.toByteArray(), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            Map<String, Object> jsonAsMap = gson.fromJson(json, Map.class);
            return UUID.fromString(jsonAsMap.get("ticketId").toString());
        } catch (IOException e) {
            throw new ParkingManagerException(500, "Unable to park the vehicle", e);
        }
    }

    public void unparkVehicle(String vehicleId, int gate) throws ParkingManagerException {
        Client client = Client.create();
        WebResource webResource = client.resource(hostAndPort + "/rest/exit?vehicleId=" + vehicleId + "&gateId=" + gate);

        ClientResponse response = webResource.accept("application/json")
                .post(ClientResponse.class);

        if (response.getStatus() != 200) {
            throw new ParkingManagerException(response.getStatus(), "Unable to park the vehicle");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = new BufferedInputStream(response.getEntityInputStream())) {
            IOUtils.copy(is, baos);
        } catch (IOException e) {
            throw new ParkingManagerException(500, "Unable to park the vehicle", e);
        }
    }

    public Set<String> getParkedVehicleIds() throws ParkingManagerException {
        Client client = Client.create();
        WebResource webResource = client.resource(hostAndPort + "/rest/parked");

        ClientResponse response = webResource.accept("application/json")
                .get(ClientResponse.class);

        if (response.getStatus() != 200) {
            throw new ParkingManagerException(response.getStatus(), "Unable to get the list of parked vehicles");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = new BufferedInputStream(response.getEntityInputStream())) {
            IOUtils.copy(is, baos);
            String json = new String(baos.toByteArray(), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            return gson.fromJson(json, Set.class);
        } catch (IOException e) {
            throw new ParkingManagerException(500, "Unable to park the vehicle", e);
        }
    }
}

package com.quitevis.parkingmanager.server.webserver;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.quitevis.parkingmanager.server.manager.ParkingManager;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * Starts a jetty server, listening to the specified port
 */
public class WebServer {
    private final ParkingManager parkingManager;
    private final int port;

    @Inject
    public WebServer(ParkingManager parkingManager,
                     @Named("server.port") int port) {
        this.parkingManager = parkingManager;
        this.port = port;
    }

    public void start() throws Exception {
        ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        handler.setContextPath("/rest");

        //ParkingManager is thread-safe so it is okay to share the instance
        handler.addServlet(new ServletHolder(new InfoServlet(parkingManager)), "/info");
        handler.addServlet(new ServletHolder(new EnterServlet(parkingManager)), "/enter");
        handler.addServlet(new ServletHolder(new ExitServlet(parkingManager)), "/exit");
        handler.addServlet(new ServletHolder(new ParkedVehiclesServlet(parkingManager)), "/parked");

        Server server = new Server(new QueuedThreadPool(100));
        server.setHandler(handler);

        ServerConnector http = new ServerConnector(server);
        http.setPort(port);
        http.setIdleTimeout(30000);

        server.addConnector(http);
        server.start();
        server.join();
    }
}

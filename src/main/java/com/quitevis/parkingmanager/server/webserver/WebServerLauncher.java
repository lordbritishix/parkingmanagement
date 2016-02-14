package com.quitevis.parkingmanager.server.webserver;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.quitevis.parkingmanager.server.manager.ParkingManagerModule;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Paths;

/**
 * Starts the http webserver using an embedded Jetty
 */
@Slf4j
public class WebServerLauncher {
    public static void main(String[] args) throws Exception {
        if (args.length <= 0) {
            log.error("Please provide the config path");
            System.exit(-1);
        }

        ParkingManagerModule parkingManagerModule = new ParkingManagerModule(Paths.get(args[0]));
        Injector injector = Guice.createInjector(parkingManagerModule);
        WebServer server = injector.getInstance(WebServer.class);
        server.start();
    }
}

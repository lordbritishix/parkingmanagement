package com.quitevis.parkingmanager.server.manager;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import com.quitevis.parkingmanager.server.logger.ParkingLogger;
import com.quitevis.parkingmanager.server.logger.Slf4jParkingLogger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Sets up the dependency injection tree
 */
public class ParkingManagerModule extends AbstractModule {
    private final Path serverConfig;
    public ParkingManagerModule(Path serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    protected void configure() {
        try {
            Properties properties = loadServerConfig(serverConfig);
            bindConstant().annotatedWith(Names.named("parking.max.slot"))
                    .to(properties.getProperty("parking.max.slot"));
            bindConstant().annotatedWith(Names.named("parking.entry.count"))
                    .to(properties.getProperty("parking.entry.count"));
            bindConstant().annotatedWith(Names.named("parking.exit.count"))
                    .to(properties.getProperty("parking.exit.count"));
            bindConstant().annotatedWith(Names.named("server.port"))
                    .to(properties.getProperty("server.port"));
        } catch (IOException e) {
            throw new RuntimeException("Unable to load the manager properties", e);
        }
    }

    @Provides
    public ParkingLogger getDefaultLogger() {
        return new Slf4jParkingLogger();
    }

    private Properties loadServerConfig(Path serverConfig) throws IOException {
        Properties properties = new Properties();

        try (InputStream is = new FileInputStream(serverConfig.toFile())) {
            properties.load(is);
        }

        return properties;
    }

}

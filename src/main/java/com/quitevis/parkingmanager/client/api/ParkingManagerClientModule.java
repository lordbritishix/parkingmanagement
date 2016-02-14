package com.quitevis.parkingmanager.client.api;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

public class ParkingManagerClientModule extends AbstractModule {
    private final Path clientConfig;

    public ParkingManagerClientModule(Path clientConfig) {
        this.clientConfig = clientConfig;
    }

    @Override
    protected void configure() {
        Properties properties;
        try {
            properties = loadClientConfig(clientConfig);
            bindConstant().annotatedWith(Names.named("server.hostname"))
                    .to(properties.getProperty("server.hostname"));
            bindConstant().annotatedWith(Names.named("server.port"))
                    .to(properties.getProperty("server.port"));
        } catch (IOException e) {
            throw new RuntimeException("Unable to find the client config file");
        }
    }

    private Properties loadClientConfig(Path clientConfig) throws IOException {
        Properties properties = new Properties();

        try (InputStream is = new FileInputStream(clientConfig.toFile())) {
            properties.load(is);
        }

        return properties;
    }

}

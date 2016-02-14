package com.quitevis.parkingmanager.server.manager;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ParkingManagerModuleTest {

    @Test
    public void injectShouldInjectParkingManager() throws IOException {
        //Write config to temp file
        Path tempConfig = Files.createTempFile("", "");
        try(InputStream is = ParkingManagerModuleTest.class.getResourceAsStream("/serverconfig.properties");
            OutputStream os = new FileOutputStream(tempConfig.toFile())) {
            IOUtils.copy(is, os);
            Injector injector = Guice.createInjector(new ParkingManagerModule(tempConfig));

            //Should throw exception if DI is not setup properly on the ParkingManagerModule
            injector.getInstance(ParkingManager.class);
        }
        finally {
            FileUtils.deleteQuietly(tempConfig.toFile());
        }
    }
}

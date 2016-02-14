package com.quitevis.parkingmanager.client;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.quitevis.parkingmanager.client.api.ParkingManagerClient;
import com.quitevis.parkingmanager.client.api.ParkingManagerClientModule;
import com.quitevis.parkingmanager.client.ui.MainScene;
import com.quitevis.parkingmanager.client.ui.MainSceneController;
import javafx.application.Application;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Paths;

@Slf4j
public class ParkingLotClientLauncher extends Application {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 700;

    @Override
    public void start(Stage primaryStage) throws Exception {
        if (getParameters().getRaw().size() <= 0) {
            log.error("Please provide the path to the config file");
            System.exit(-1);
        }

        Injector injector = Guice.createInjector(
                new ParkingManagerClientModule(Paths.get(getParameters().getRaw().get(0))));

        primaryStage.setTitle("Jim's Parking Lot Management");
        primaryStage.setWidth(WIDTH);
        primaryStage.setHeight(HEIGHT);
        MainSceneController controller = new MainSceneController(injector.getInstance(ParkingManagerClient.class));
        MainScene scene = new MainScene(controller);
        controller.setScene(scene);

        primaryStage.setScene(scene);
        primaryStage.show();

        controller.onSceneShow();
    }
}

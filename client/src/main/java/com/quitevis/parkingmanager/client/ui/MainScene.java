package com.quitevis.parkingmanager.client.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Form to display controls to talk to the Parking Management server
 */
public class MainScene extends Scene {
    private final MainSceneController controller;
    private final Label maxCapacity;
    private final Label currentCapacity;
    private final Label capacityLeft;
    private final Label entryCount;
    private final Label exitCount;
    private final Button startStopSimulation;
    private final TextFlow eventLog;
    private static final int MAX_EVENT_LOG_LINES = 35;

    public MainScene(MainSceneController controller) {
        super(new VBox(5.0d));
        this.controller = controller;

        this.maxCapacity = new Label();
        this.currentCapacity = new Label();
        this.capacityLeft = new Label();
        this.entryCount = new Label();
        this.exitCount = new Label();
        this.eventLog = new TextFlow();
        this.eventLog.setMaxHeight(100.0d);

        add(new HBox(new Label("Max Capacity: "), maxCapacity));
        add(new HBox(new Label("Current Capacity: "), currentCapacity));
        add(new HBox(new Label("Capacity Left: "), capacityLeft));
        add(new HBox(new Label("Entry Count: "), entryCount));
        add(new HBox(new Label("Exit Count: "), exitCount));

        HBox spacer = new HBox();
        spacer.setPadding(new Insets(8));
        add(spacer);

        Button parkCar = new Button("Park a random car");
        parkCar.setOnMouseClicked(p -> {
            try {
                controller.parkRandomCarButtonPressed();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Button exitCar = new Button("Exit a random car");
        exitCar.setOnMouseClicked(p -> {
            try {
                controller.exitRandomCarButtonPressed();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        startStopSimulation = new Button("Simulate random cars entering and exiting the parking lot");
        startStopSimulation.setOnMouseClicked(p -> controller.startStopSimulationButtonPressed());

        HBox buttonBox = new HBox(5.0d);
        buttonBox.getChildren().addAll(parkCar, exitCar, startStopSimulation);
        add(buttonBox);
        HBox spacer2 = new HBox();
        spacer.setPadding(new Insets(8));
        add(spacer2);
        add(eventLog);

        getPane().setPadding(new Insets(10.0d));
    }

    public void appendTextToEventLog(String text) {
        eventLog.getChildren().add(0, new Text(text + "\n"));
        if (eventLog.getChildren().size() >= MAX_EVENT_LOG_LINES) {
            eventLog.getChildren().remove(eventLog.getChildren().size() - 1);
        }
    }

    public void setStartSimulationText() {
        startStopSimulation.setText("Simulate random cars entering and exiting the parking lot");
    }

    public void setStopSimulationText() {
        startStopSimulation.setText("Stop the simulation");
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity.setText(String.valueOf(maxCapacity));
    }

    public void setCurrentCapacity(int currentCapacity) {
        this.currentCapacity.setText(String.valueOf(currentCapacity));
    }

    public void setCapacityLeft(int capacityLeft) {
        this.capacityLeft.setText(String.valueOf(capacityLeft));
    }

    public void setEntryCount(int entryCount) {
        this.entryCount.setText(String.valueOf(entryCount));
    }

    public void setExitCount(int exitCount) {
        this.exitCount.setText(String.valueOf(exitCount));
    }


    private void add(Node node) {
        getPane().getChildren().add(node);
    }

    private VBox getPane() {
        return (VBox) getRoot();
    }

    /**
     * Shows a modal loading dialog. Runs longRunningTask on a different thread. When complete, it hides the loading dialog
     * and then executes callWhenComplete
     */
    public void runTaskOnBackgroundThreadWithLoadingDialog(String message, Supplier<String> longRunningTask, BiConsumer<String, Throwable> callWhenComplete) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.setHeaderText(null);

        CompletableFuture
                .supplyAsync(longRunningTask)
                .whenComplete((p, q) -> {
                    Platform.runLater(() -> alert.hide());
                    callWhenComplete.accept(p, q);
                });
        alert.showAndWait();
    }
}

package com.quitevis.parkingmanager.client.ui;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.quitevis.parkingmanager.client.api.ParkingManagerClient;
import com.quitevis.parkingmanager.client.api.ParkingManagerException;
import com.quitevis.parkingmanager.model.ParkingManagerInfo;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class MainSceneController {
    private MainScene scene;
    private final ParkingManagerClient client;
    private volatile boolean isSimulating = false;
    private volatile int entryCount;
    private volatile int exitCount;

    @Inject
    public MainSceneController(ParkingManagerClient client) {
        this.client = client;
    }

    public void setScene(MainScene scene) {
        this.scene = scene;
    }

    public void onSceneShow() {
        try {
            updateValues();
        } catch (Exception e) {
            scene.appendTextToEventLog("Unable to contact the Parking Management server");
        }
    }

    private void updateValues() throws ParkingManagerException {
        ParkingManagerInfo info = client.getInfo();
        entryCount = info.getEntryCount();
        exitCount = info.getExitCount();

        //Useful asserts to test if invariants would hold
        assert(info.getCurrentCapacity() <= info.getMaxCapacity());
        assert(info.getCapacityLeft() >= 0);

        Platform.runLater(() -> {
            scene.setCapacityLeft(info.getCapacityLeft());
            scene.setCurrentCapacity(info.getCurrentCapacity());
            scene.setMaxCapacity(info.getMaxCapacity());
            scene.setEntryCount(info.getEntryCount());
            scene.setExitCount(info.getExitCount());
        });
    }

    public void parkRandomCarButtonPressed() throws Exception {
        UUID vehicleId = UUID.randomUUID();
        int gate = RandomUtils.nextInt(0, entryCount);

        scene.runTaskOnBackgroundThreadWithLoadingDialog("Please wait",
                () -> {
                    try {
                        return parkVehicle(vehicleId.toString(), gate).toString();
                    } catch (ParkingManagerException e) {
                        throw new RuntimeException(e);
                    }
                }, (p, q) -> Platform.runLater(() -> {
                    if (q != null) {
                        if (p != null) {
                            scene.appendTextToEventLog("Unable to park the vehicle: " + vehicleId);
                        }
                        else {
                            scene.appendTextToEventLog("Unable to park the vehicle - Parking full: " + vehicleId);
                        }
                    } else {
                        scene.appendTextToEventLog("Vehicle parked: " + p);
                    }
                }));
    }

    public void exitRandomCarButtonPressed() throws Exception {
        scene.runTaskOnBackgroundThreadWithLoadingDialog("Please wait",
                () -> {
                    try {
                        return unparkRandomVehicle();
                    } catch (ParkingManagerException e) {
                        throw new RuntimeException(e);
                    }
                }, (vehicleId, q) -> Platform.runLater(() -> {
                    if (q != null) {
                        if (vehicleId != null) {
                            scene.appendTextToEventLog("Unable to unpark the vehicle: " + vehicleId);
                        }
                        else {
                            Platform.runLater(() -> scene.appendTextToEventLog("Unable to unpark the vehicle"));
                        }
                    } else {
                        scene.appendTextToEventLog("Vehicle unparked: " + vehicleId);
                    }
                }));
    }

    private UUID parkVehicle(String vehicleId, int gate) throws ParkingManagerException {
        UUID uuid = client.parkVehicle(vehicleId.toString(), gate);
        updateValues();
        return uuid;
    }

    private String unparkRandomVehicle() throws ParkingManagerException {
        Set<String> vehicleIds = client.getParkedVehicleIds();
        if (vehicleIds.isEmpty()) {
            scene.appendTextToEventLog("Unable to unpark park, no vehicles are parked");
            return null;
        }

        String[] vehicleIdsAsArray = vehicleIds.stream().toArray(p -> new String[p]);
        int random = RandomUtils.nextInt(0, vehicleIdsAsArray.length);
        String vehicleId = vehicleIdsAsArray[random];
        client.unparkVehicle(vehicleId, RandomUtils.nextInt(0, exitCount));
        updateValues();
        return vehicleId;
    }


    public void startStopSimulationButtonPressed() {
        if (isSimulating) {
            scene.setStartSimulationText();
        } else {
            scene.setStopSimulationText();
        }

        isSimulating = !isSimulating;

        if (!isSimulating) {
            return;
        }

        //Parking
        CompletableFuture.runAsync(() -> {
            while(isSimulating) {
                try {
                    Thread.sleep(RandomUtils.nextLong(100L, 2000L));
                } catch (InterruptedException e) {
                }
                int nextBatch = RandomUtils.nextInt(0, entryCount);
                List<Thread> threads = Lists.newArrayList();
                for (int x = 0; x < nextBatch; ++x) {
                    Thread t = new Thread(() -> {
                        UUID vehicleId = UUID.randomUUID();
                        int gate = RandomUtils.nextInt(0, entryCount);
                        try {
                            parkVehicle(vehicleId.toString(), gate);
                            Platform.runLater(() -> scene.appendTextToEventLog("Vehicle parked: " + vehicleId.toString() + " entered gate: " + gate));
                        } catch (ParkingManagerException e) {
                            if (vehicleId != null) {
                                Platform.runLater(() -> scene.appendTextToEventLog("Unable to park the vehicle - already full: " + vehicleId.toString()));
                            }
                            else {
                                Platform.runLater(() -> scene.appendTextToEventLog("Unable to park the vehicle"));
                            }
                        }
                    });
                    threads.add(t);
                    t.start();
                }

                threads.stream().forEach(p -> {
                    try {
                        p.join();
                    } catch (InterruptedException e) {
                    }
                });
            }
        });

        //Unparking
        CompletableFuture.runAsync(() -> {
            while(isSimulating) {
                try {
                    Thread.sleep(RandomUtils.nextLong(2000L, 5000L));
                } catch (InterruptedException e) {
                }
                int nextBatch = RandomUtils.nextInt(0, exitCount);
                    List<Thread> threads = Lists.newArrayList();
                    for (int x = 0; x < nextBatch; ++x) {
                        Thread t = new Thread(() -> {
                            try {
                                String vehicleId = unparkRandomVehicle();
                                scene.appendTextToEventLog("Vehicle unparked: " + vehicleId);
                            }
                            catch(Exception e) {
                                Platform.runLater(() -> scene.appendTextToEventLog("Unable to unpark park the vehicle"));
                            }
                        });
                        threads.add(t);
                        t.start();
                    }

                    threads.stream().forEach(p -> {
                        try {
                            p.join();
                        } catch (InterruptedException e) {
                        }
                    });
            }
        });
    }
}

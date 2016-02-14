package com.quitevis.parkingmanager.server.manager;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.quitevis.parkingmanager.server.logger.ParkingLogger;
import com.quitevis.parkingmanager.model.VehicleRecord;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ParkingManagerTest {
    @Mock
    VehicleRecord vehicleRecord;

    @Mock
    ParkingLogger logger;

    @Test
    public void enterAndExitShouldResultInNoVehiclesParked() throws ExecutionException, InterruptedException {
        ParkingManager manager = new ParkingManager(1, 1, 1, logger);
        manager.enter(0, vehicleRecord).get();
        assertThat(manager.getCapacityLeft(), is(0));
        manager.exit(0, vehicleRecord).get();
        assertThat(manager.getCapacityLeft(), is(1));

        verify(logger).log(vehicleRecord, ParkingLogger.State.PARKED);
        verify(logger).log(vehicleRecord, ParkingLogger.State.EXITED_PARKING);
    }

    @Test
    public void enterWhenFullThrowsException() throws ExecutionException, InterruptedException {
        ParkingManager manager = new ParkingManager(1, 1, 1, logger);
        manager.enter(0, vehicleRecord).get();

        try {
            manager.enter(0, vehicleRecord).get();
            fail();
        }
        catch(Exception e) {
            //Exception expected
        }

        verify(logger).log(vehicleRecord, ParkingLogger.State.PARKED);
    }

    @Test
    public void exitWithNoVehicleParkedShouldThrowException() throws ExecutionException, InterruptedException {
        ParkingManager manager = new ParkingManager(1, 1, 1, logger);
        try {
            manager.exit(0, vehicleRecord).get();
            fail();
        }
        catch(Exception e) {
            //Expect exception
        }

        assertThat(manager.getCapacityLeft(), is(1));
        verify(logger, never()).log(vehicleRecord, ParkingLogger.State.PARKED);
        verify(logger, never()).log(vehicleRecord, ParkingLogger.State.EXITED_PARKING);
    }

    @Test
    public void enterWithSameVehicleShouldThrowException() throws ExecutionException, InterruptedException {
        ParkingManager manager = new ParkingManager(1, 1, 1, logger);
        manager.enter(0, vehicleRecord).get();

        try {
            manager.enter(0, vehicleRecord).get();
            fail();
        }
        catch(Exception e) {
            //Expect exception
        }

        verify(logger).log(vehicleRecord, ParkingLogger.State.PARKED);
    }

    @Test
    public void enterShouldQueueMultipleCarsInSameGate() throws ExecutionException, InterruptedException {
        int count = 100;
        ParkingManager manager = new ParkingManager(count, 1, 1, logger);
        List<VehicleRecord> parkedVehicleRecords = Lists.newArrayList();
        CountDownLatch latch = new CountDownLatch(count);

        when(logger.log(any(VehicleRecord.class), any(ParkingLogger.State.class))).then(p -> {
            parkedVehicleRecords.add((VehicleRecord) p.getArguments()[0]);
            //Introduce artificial delay to properly observe queueing of vehicleRecords on the same gate
            Thread.sleep(10);
            latch.countDown();
            return  true;
        });

        List<VehicleRecord> vehicleRecords = Lists.newArrayList();

        for (int x = 0; x < count; ++x) {
            VehicleRecord v = mock(VehicleRecord.class);
            vehicleRecords.add(v);
            manager.enter(0, v);
        }

        //Wait for all the vehicleRecords to be processed
        latch.await(5, TimeUnit.MINUTES);

        //The vehicleRecords should be logged in the same order as they arrive
        assertThat(parkedVehicleRecords, is(vehicleRecords));
        assertThat(manager.getCapacityLeft(), is(0));
    }

    @Test
    public void enterOnMultipleGatesShouldNotResultInOverCapacity() throws ExecutionException, InterruptedException {
        int count = 100;
        int capacity = 50;
        int entryCount = 5;
        ParkingManager manager = new ParkingManager(capacity, entryCount, 1, logger);
        CountDownLatch latch = new CountDownLatch(count);

        when(logger.log(any(VehicleRecord.class), any(ParkingLogger.State.class))).then(p -> {
            Thread.sleep(10);
            latch.countDown();
            return  true;
        });

        for (int x = 0; x < count; ++x) {
            VehicleRecord v = mock(VehicleRecord.class);
            manager.enter(RandomUtils.nextInt(0, entryCount), v);
        }

        //Wait for all the vehicles to be processed
        latch.await(5, TimeUnit.MINUTES);

        assertThat(manager.getCapacityLeft(), is(0));
        assertThat(manager.getCurrentCapacity(), is(capacity));
    }

    @Test
    public void exitOnMultipleGatesShouldNotResultInNegativeCurrentCapacity() throws ExecutionException, InterruptedException {
        int count = 100;
        int exitCount = 5;
        ParkingManager manager = new ParkingManager(count, 1, exitCount, logger);
        CountDownLatch latch = new CountDownLatch(count * 2);

        when(logger.log(any(VehicleRecord.class), any(ParkingLogger.State.class))).then(p -> {
            Thread.sleep(10);
            latch.countDown();
            return  true;
        });

        List<VehicleRecord> vehicleRecords = Lists.newArrayList();

        for (int x = 0; x < count; ++x) {
            VehicleRecord v = mock(VehicleRecord.class);
            vehicleRecords.add(v);
            manager.enter(0, v).get();
        }

        for (VehicleRecord vehicleRecord : vehicleRecords) {
            manager.exit(RandomUtils.nextInt(0, exitCount), vehicleRecord);
        }

        //Wait for all the vehicleRecords to be processed
        latch.await(5, TimeUnit.MINUTES);

        assertThat(manager.getCapacityLeft(), is(count));
        assertThat(manager.getCurrentCapacity(), is(0));
    }

    @Test
    public void enterAndExitOnMultipleGatesShouldNotViolateInvariants() throws ExecutionException, InterruptedException {
        int count = 100;
        int exitCount = 5;
        int entryCount = 5;
        ParkingManager manager = new ParkingManager(count, entryCount, exitCount, logger);
        CountDownLatch latch = new CountDownLatch(count * 2);
        BlockingQueue<VehicleRecord> vehicleRecordQueue = Queues.newLinkedBlockingQueue();


        when(logger.log(any(VehicleRecord.class), any(ParkingLogger.State.class))).then(p -> {
            ParkingLogger.State state = (ParkingLogger.State)p.getArguments()[1];
            VehicleRecord vehicleRecord = (VehicleRecord) p.getArguments()[0];

            //Once the car has parked, put it on the blocking queue for consumption
            if (state == ParkingLogger.State.PARKED) {
                vehicleRecordQueue.add(vehicleRecord);
            }

            //Introduce randomness to simulate good activity of entering / exiting cars
            Thread.sleep(RandomUtils.nextInt(0, 20));
            latch.countDown();
            return  true;
        });

        //Enter vehicles, asynchronously (Producer)
        for (int x = 0; x < count; ++x) {
            VehicleRecord v = mock(VehicleRecord.class);
            manager.enter(RandomUtils.nextInt(0, entryCount), v);
        }

        //Exit vehicles, asynchronously (Consumer)
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    //Exit vehicles as soon as they arrive
                    VehicleRecord vehicleRecord = vehicleRecordQueue.take();
                    manager.exit(RandomUtils.nextInt(0, exitCount), vehicleRecord);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        t.start();

        //Wait for all the vehicles to enter and exit
        latch.await(5, TimeUnit.MINUTES);

        assertThat(manager.getCapacityLeft(), is(count));
        assertThat(manager.getCurrentCapacity(), is(0));

        //Stop the blocking queue
        t.interrupt();
    }

}

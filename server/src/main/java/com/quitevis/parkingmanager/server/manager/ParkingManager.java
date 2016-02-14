package com.quitevis.parkingmanager.server.manager;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.quitevis.parkingmanager.model.VehicleRecord;
import com.quitevis.parkingmanager.server.logger.ParkingLogger;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * The ParkingManager class is responsible for managing the parking requests which are:
 * 1. Entering the parking lot
 * 2. Exiting the parking lot
 * <p>
 * There can be multiple entries and exits on the parking lot which that means multiple cars can enter and exit the parking
 * lot at the same time. To model such behavior, each of the entry and exit will be assigned its own thread. If for example
 * 2 cars exit the same exit gate number, the 2nd car will be blocked until after the 1st car has exited.
 * <p>
 * Valid entry / exit ids are 0 - (entryCount - 1) and 0 - (exitExecutorMap - 1)
 * <p>
 * Mutable states are parkedCarCounter and vehiclesParked
 * They are guarded by a ReadWriteLock.
 */
@Slf4j
@Singleton
public class ParkingManager {
    private volatile int parkedCarCounter;
    private final Set<VehicleRecord> vehiclesParked;
    private final Map<Integer, ExecutorService> entryExecutorMap;
    private final Map<Integer, ExecutorService> exitExecutorMap;
    private final ParkingLogger parkingLogger;
    private final int capacity;
    private final int entryCount;
    private final int exitCount;

    //Picking the approach where the write lock is exclusive can have scalability problems later if the enter / exit
    //operations become more expensive - but for now, it is really fast so stick with a simple and clear solution.
    private final ReadWriteLock readWriteLock;

    @Inject
    public ParkingManager(
            @Named("parking.max.slot") int capacity,
            @Named("parking.entry.count") int entryCount,
            @Named("parking.exit.count") int exitCount,
            ParkingLogger parkingLogger) {
        this.capacity = capacity;
        this.entryCount = entryCount;
        this.exitCount = exitCount;
        this.entryExecutorMap = Maps.newHashMap();
        this.exitExecutorMap = Maps.newHashMap();
        this.vehiclesParked = Sets.newConcurrentHashSet();
        this.readWriteLock = new ReentrantReadWriteLock();
        this.parkingLogger = parkingLogger;

        //We don't want multiple cars to enter the same gate at the same time, so use a single thread executor
        //with unbounded queue
        for (int x = 0; x < entryCount; ++x) {
            entryExecutorMap.put(x, Executors.newSingleThreadExecutor());
        }

        for (int x = 0; x < exitCount; ++x) {
            exitExecutorMap.put(x, Executors.newSingleThreadExecutor());
        }
    }

    /**
     * Parks a car. If multiple cars enter the same gate, they are queued in the request order.
     * Valid values for the entryGateNumber are 0 - (entryCount - 1)
     * This is done in an asynchronous manner so that other cars entering other gates are not blocked.
     * <p>
     * Returns a Future that can be used to monitor for completion. When the future completes, a ticket, in
     * the form of a UUID is returned.
     * Throws a RuntimeException if:
     * 1. An attempt to park the same vehicleRecord was made
     * 2. For some reason, we are over capacity (serious bug!)
     */
    public CompletableFuture<UUID> enter(int entryGateNumber, VehicleRecord vehicleRecord) {
        if (entryGateNumber >= entryCount) {
            throw new IllegalArgumentException("The provided entry gate number does not exist.");
        }

        return CompletableFuture.supplyAsync(() -> {
            Lock lock = readWriteLock.writeLock();
            try {
                lock.lock();
                if (vehiclesParked.contains(vehicleRecord)) {
                    throw new IllegalArgumentException("This vehicleRecord is already parked.");
                }

                if (parkedCarCounter >= capacity) {
                    throw new IllegalStateException("The parking lot is already full.");
                }

                vehiclesParked.add(vehicleRecord);
                parkedCarCounter++;

                //Throw assertion error if invariant is violated
                assert (parkedCarCounter <= capacity);
            } finally {
                lock.unlock();
            }

            UUID ticketId = UUID.randomUUID();
            vehicleRecord.setTicketId(ticketId);
            vehicleRecord.setDateEntered(LocalDateTime.now(ZoneOffset.UTC));
            parkingLogger.log(vehicleRecord, ParkingLogger.State.PARKED);

            return UUID.randomUUID();
        }, entryExecutorMap.get(entryGateNumber))
                .exceptionally(e -> {
                    parkingLogger.log(vehicleRecord, ParkingLogger.State.UNABLE_TO_PARK);
                    throw new RuntimeException(e);
                });
    }

    /**
     * Unparks a car. If multiple cars exits the same gate, they are queued in the request order.
     * Valid values for the exitGateNumber are 0 - (exitCount - 1).
     * This is done in an asynchronous manner so that other cars exiting other gates are not blocked.
     * <p>
     * Throws a RuntimeException if:
     * 1. The car is not really parked but it tries to exit the parking lot
     * 2. The parkedCarCounter is already zero before the car exits (serious bug)
     */
    public CompletableFuture<Void> exit(int exitGateNumber, VehicleRecord vehicleRecord) {
        if (exitGateNumber > exitCount) {
            throw new IllegalArgumentException("The provided exit gate number does not exist.");
        }

        return CompletableFuture.runAsync(() -> {
            Lock lock = readWriteLock.writeLock();
            try {
                lock.lock();

                if (!vehiclesParked.contains(vehicleRecord)) {
                    throw new IllegalArgumentException("The provided vehicleRecord is not found.");
                }

                vehiclesParked.remove(vehicleRecord);
                parkedCarCounter--;

                //Throw assertion error if invariant is violated
                assert (parkedCarCounter >= 0);
            } finally {
                lock.unlock();
            }

            vehicleRecord.setDateExited(LocalDateTime.now(ZoneOffset.UTC));
            parkingLogger.log(vehicleRecord, ParkingLogger.State.EXITED_PARKING);
        }, exitExecutorMap.get(exitGateNumber))
                .exceptionally(e -> {
                    throw new RuntimeException(e);
                });
    }

    /**
     * Returns the number of cars parked
     */
    public int getCurrentCapacity() {
        Lock lock = readWriteLock.readLock();
        try {
            lock.lock();
            return vehiclesParked.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the max capacity of the parking lot
     */
    public int getMaxCapacity() {
        Lock lock = readWriteLock.readLock();
        try {
            lock.lock();
            return capacity;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the capacity left on the parking lot
     */
    public int getCapacityLeft() {
        Lock lock = readWriteLock.readLock();
        try {
            lock.lock();
            return capacity - parkedCarCounter;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the parked vehicles
     */
    public Set<VehicleRecord> getParkedVehicleIds() {
        Lock lock = readWriteLock.readLock();
        try {
            lock.lock();
            return ImmutableSet.copyOf(vehiclesParked);
        }
        finally {
            lock.unlock();
        }

    }

    public int getEntryCount() {
        return entryCount;
    }

    public int getExitCount() {
        return exitCount;
    }

    /**
     * Shuts down the executor services associated with the entry and exit gates and cleans them.
     * Once close is called, this object cannot be used anymore to manage vehicles entering / exiting
     */
    public void close() {
        Consumer<ExecutorService> executorServiceKiller = p -> {
            p.shutdown();
            try {
                p.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                //Swallow exception, don't care if the blocking call above is interrupted
                log.error("Await termination got interrupted", e);
            }
        };

        entryExecutorMap.values().stream().forEach(executorServiceKiller);
        exitExecutorMap.values().stream().forEach(executorServiceKiller);
        entryExecutorMap.clear();
        exitExecutorMap.clear();
    }

}

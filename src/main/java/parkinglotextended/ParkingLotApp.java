package parkinglotextended;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/*
=============================
PARKING LOT LOW LEVEL DESIGN
=============================

Features Supported
------------------
1. Multi Floor Parking
2. Vehicle Types (CAR, BIKE)
3. Entry Gate -> Spot Allocation -> Ticket Creation
4. Exit Gate -> Price Calculation -> Payment -> Spot Release
5. Display Board for available spots
6. Payment transactions

Design Patterns Used
--------------------
1. Strategy Pattern -> Spot Selection
2. Strategy Pattern -> Pricing
3. Strategy Pattern -> Payment

Thread Safety
-------------
ParkingSpot.occupy() synchronized to avoid race condition when
multiple cars attempt to occupy same spot.

Main Components
---------------
ParkingLot
ParkingFloor
ParkingSpot
Vehicle
ParkingTicket
EntryGate
ExitGate

Services
--------
ParkingSpotService
TicketService
PaymentService
*/

enum VehicleType {
    CAR,
    BIKE
}

enum SpotType {
    CAR,
    BIKE
}

enum TicketStatus {
    ACTIVE,
    CLOSED
}

enum PaymentType {
    UPI,
    CASH
}

enum PaymentStatus {
    SUCCESS,
    FAILED
}

/* =========================
   VEHICLE
   ========================= */

class Vehicle {

    String vehicleNumber;
    VehicleType type;

    Vehicle(String vehicleNumber, VehicleType type) {
        this.vehicleNumber = vehicleNumber;
        this.type = type;
    }
}

/* =========================
   PARKING SPOT
   ========================= */

class ParkingSpot {

    int id;
    int floorNo;
    SpotType type;
    boolean available;

    ParkingSpot(int id, int floorNo, SpotType type) {
        this.id = id;
        this.floorNo = floorNo;
        this.type = type;
        this.available = true;
    }

    // prevent race condition when two threads try to occupy same spot
    synchronized boolean occupy() {

        if (!available) return false;

        available = false;
        return true;
    }

    void free() {
        available = true;
    }
}

/* =========================
   PARKING FLOOR
   ========================= */

class ParkingFloor {

    int floorNo;
    List<ParkingSpot> spots;

    ParkingFloor(int floorNo) {
        this.floorNo = floorNo;
        this.spots = new ArrayList<>();
    }

    void addSpot(ParkingSpot spot) {
        spots.add(spot);
    }
}

/* =========================
   DISPLAY BOARD
   Shows free spots per type
   ========================= */

class DisplayBoard {

    Map<SpotType, Integer> freeSpots = new HashMap<>();

    void update(SpotType type, int count) {
        freeSpots.put(type, count);
    }

    void show() {

        System.out.println("Display Board:");

        for (SpotType type : freeSpots.keySet()) {

            System.out.println(
                    type + " Spots Available : " + freeSpots.get(type)
            );
        }
    }
}

/* =========================
   STRATEGY: SPOT SELECTION
   ========================= */

interface ParkingSpotSelectionStrategy {

    ParkingSpot selectSpot(List<ParkingSpot> spots, VehicleType type);
}

class FirstAvailableStrategy implements ParkingSpotSelectionStrategy {

    public ParkingSpot selectSpot(List<ParkingSpot> spots, VehicleType type) {

        for (ParkingSpot spot : spots) {

            if (spot.available &&
                    spot.type.name().equals(type.name())) {

                if (spot.occupy()) {
                    return spot;
                }
            }
        }

        return null;
    }
}

/* =========================
   PARKING SPOT SERVICE
   Handles spot allocation
   ========================= */

class ParkingSpotService {

    Map<Integer, ParkingSpot> spotMap = new ConcurrentHashMap<>();

    Map<SpotType, List<ParkingSpot>> typeIndex = new HashMap<>();

    ParkingSpotSelectionStrategy strategy;

    DisplayBoard displayBoard;

    ParkingSpotService(ParkingSpotSelectionStrategy strategy,
                       DisplayBoard displayBoard) {

        this.strategy = strategy;
        this.displayBoard = displayBoard;
    }

    void addSpot(ParkingSpot spot) {

        spotMap.put(spot.id, spot);

        typeIndex.putIfAbsent(spot.type, new ArrayList<>());

        typeIndex.get(spot.type).add(spot);
    }

    ParkingSpot findSpot(VehicleType type) {

        List<ParkingSpot> spots = typeIndex.get(SpotType.valueOf(type.name()));

        return strategy.selectSpot(spots, type);
    }

    void freeSpot(int spotId) {

        ParkingSpot spot = spotMap.get(spotId);

        spot.free();
    }

    // calculate availability per floor
    Map<Integer, Map<SpotType, Integer>> getAvailabilityPerFloor() {

        Map<Integer, Map<SpotType, Integer>> result = new HashMap<>();

        for (ParkingSpot spot : spotMap.values()) {

            if (!spot.available) continue;

            result.putIfAbsent(spot.floorNo, new HashMap<>());

            Map<SpotType, Integer> floorMap = result.get(spot.floorNo);

            floorMap.put(
                    spot.type,
                    floorMap.getOrDefault(spot.type, 0) + 1
            );
        }

        return result;
    }
}

/* =========================
   PARKING TICKET
   ========================= */

class ParkingTicket {

    String id;

    Vehicle vehicle;

    int spotId;

    int floorNo;

    LocalDateTime entryTime;

    LocalDateTime exitTime;

    TicketStatus status;

    ParkingTicket(String id, Vehicle vehicle, ParkingSpot spot) {

        this.id = id;
        this.vehicle = vehicle;
        this.spotId = spot.id;
        this.floorNo = spot.floorNo;

        this.entryTime = LocalDateTime.now();
        this.status = TicketStatus.ACTIVE;
    }
}

/* =========================
   PRICE CALCULATION
   ========================= */

interface PriceCalculator {

    double calculatePrice(ParkingTicket ticket);
}

class HourlyPriceCalculator implements PriceCalculator {

    int ratePerHour = 10;

    public double calculatePrice(ParkingTicket ticket) {

        long minutes =
                Duration.between(ticket.entryTime, LocalDateTime.now()).toMinutes();

        long hours = (long) Math.ceil(minutes / 60.0);

        return hours * ratePerHour;
    }
}

/* =========================
   TICKET SERVICE
   ========================= */

class TicketService {

    Map<String, ParkingTicket> ticketMap = new ConcurrentHashMap<>();

    PriceCalculator priceCalculator;

    ParkingSpotService spotService;

    TicketService(PriceCalculator priceCalculator,
                  ParkingSpotService spotService) {

        this.priceCalculator = priceCalculator;
        this.spotService = spotService;
    }

    ParkingTicket createTicket(Vehicle vehicle, ParkingSpot spot) {

        String id = UUID.randomUUID().toString();

        ParkingTicket ticket = new ParkingTicket(id, vehicle, spot);

        ticketMap.put(id, ticket);

        return ticket;
    }

    double calculatePrice(String ticketId) {

        ParkingTicket ticket = ticketMap.get(ticketId);

        return priceCalculator.calculatePrice(ticket);
    }

    void closeTicket(String ticketId) {

        ParkingTicket ticket = ticketMap.get(ticketId);

        ticket.exitTime = LocalDateTime.now();

        ticket.status = TicketStatus.CLOSED;

        spotService.freeSpot(ticket.spotId);
    }
}

/* =========================
   PAYMENT ENTITY
   ========================= */

class Payment {

    String id;

    String ticketId;

    double amount;

    PaymentType type;

    PaymentStatus status;

    Payment(String ticketId, double amount, PaymentType type) {

        this.id = UUID.randomUUID().toString();

        this.ticketId = ticketId;
        this.amount = amount;
        this.type = type;
    }
}

/* =========================
   PAYMENT PROCESSORS
   ========================= */

interface PaymentProcessor {

    boolean pay(double amount);
}

class UpiPaymentProcessor implements PaymentProcessor {

    public boolean pay(double amount) {

        System.out.println("UPI Payment Success : " + amount);

        return true;
    }
}

class CashPaymentProcessor implements PaymentProcessor {

    public boolean pay(double amount) {

        System.out.println("Cash Payment Success : " + amount);

        return true;
    }
}

/* =========================
   PAYMENT SERVICE
   ========================= */

class PaymentService {

    Map<PaymentType, PaymentProcessor> processorMap;

    PaymentService() {

        processorMap = new HashMap<>();

        processorMap.put(PaymentType.UPI, new UpiPaymentProcessor());
        processorMap.put(PaymentType.CASH, new CashPaymentProcessor());
    }

    Payment processPayment(String ticketId, double amount, PaymentType type) {

        Payment payment = new Payment(ticketId, amount, type);

        boolean success = processorMap.get(type).pay(amount);

        payment.status = success ?
                PaymentStatus.SUCCESS :
                PaymentStatus.FAILED;

        return payment;
    }
}

/* =========================
   ENTRY GATE
   ========================= */

class EntryGate {

    ParkingSpotService spotService;

    TicketService ticketService;

    EntryGate(ParkingSpotService spotService,
              TicketService ticketService) {

        this.spotService = spotService;
        this.ticketService = ticketService;
    }

    ParkingTicket parkVehicle(Vehicle vehicle) {

        ParkingSpot spot = spotService.findSpot(vehicle.type);

        if (spot == null) {

            throw new RuntimeException("Parking Full");
        }

        return ticketService.createTicket(vehicle, spot);
    }
}

/* =========================
   EXIT GATE
   ========================= */

class ExitGate {

    TicketService ticketService;

    PaymentService paymentService;

    ExitGate(TicketService ticketService,
             PaymentService paymentService) {

        this.ticketService = ticketService;
        this.paymentService = paymentService;
    }

    void exitVehicle(String ticketId, PaymentType type) {

        // Step 1 calculate price
        double amount = ticketService.calculatePrice(ticketId);

        // Step 2 payment
        Payment payment =
                paymentService.processPayment(ticketId, amount, type);

        if (payment.status == PaymentStatus.FAILED) {

            throw new RuntimeException("Payment Failed");
        }

        // Step 3 close ticket and free spot
        ticketService.closeTicket(ticketId);
    }
}

/* =========================
   MAIN APPLICATION
   ========================= */

public class ParkingLotApp {

    public static void main(String[] args) {

        DisplayBoard board = new DisplayBoard();

        ParkingSpotSelectionStrategy strategy =
                new FirstAvailableStrategy();

        ParkingSpotService spotService =
                new ParkingSpotService(strategy, board);

        PriceCalculator calculator =
                new HourlyPriceCalculator();

        TicketService ticketService =
                new TicketService(calculator, spotService);

        PaymentService paymentService =
                new PaymentService();

        EntryGate entryGate =
                new EntryGate(spotService, ticketService);

        ExitGate exitGate =
                new ExitGate(ticketService, paymentService);

        // create parking spots
        spotService.addSpot(new ParkingSpot(1, 1, SpotType.CAR));
        spotService.addSpot(new ParkingSpot(2, 1, SpotType.CAR));
        spotService.addSpot(new ParkingSpot(3, 1, SpotType.BIKE));

        // vehicle enters
        Vehicle car = new Vehicle("KA01AB1234", VehicleType.CAR);

        ParkingTicket ticket = entryGate.parkVehicle(car);

        // vehicle exits
        exitGate.exitVehicle(ticket.id, PaymentType.UPI);
    }
}
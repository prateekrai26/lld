package parkingLot;

/*
Parking Lot LLD

Supports:
- Multi floor parking
- Vehicle types (CAR, BIKE)
- Entry Gate -> Spot allocation -> Ticket generation
- Exit Gate -> Price calculation -> Payment -> Exit

Key Components
ParkingLot
ParkingSpot
Vehicle
ParkingTicket
EntryGate
ExitGate

Services
ParkingSpotService
TicketService
PaymentService

Patterns
Strategy Pattern -> Spot Selection
Strategy Pattern -> Pricing
Strategy Pattern -> Payment
*/

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

enum VehicleType{
    CAR, BIKE
}

class Vehicle{
    String vehicleNumber;
    VehicleType type;

    Vehicle(String vehicleNumber, VehicleType type){
        this.vehicleNumber = vehicleNumber;
        this.type = type;
    }
}

enum SpotType{
    CAR, BIKE
}

class ParkingSpot{
    int id;
    int floorNo;
    SpotType type;
    boolean isAvailable;

    ParkingSpot(int id, int floorNo, SpotType type){
        this.id = id;
        this.floorNo = floorNo;
        this.type = type;
        this.isAvailable = true;
    }

    synchronized boolean occupy(){
        if(!isAvailable) return false;
        isAvailable = false;
        return true;
    }

    void free(){
        isAvailable = true;
    }
}

interface ParkingSpotSelectionStrategy{
    ParkingSpot selectSpot(List<ParkingSpot> spots, VehicleType type);
}

class FirstAvailableStrategy implements ParkingSpotSelectionStrategy{

    public ParkingSpot selectSpot(List<ParkingSpot> spots, VehicleType type){

        for(ParkingSpot spot : spots){

            if(spot.isAvailable && spot.type.name().equals(type.name())){
                if(spot.occupy()){
                    return spot;
                }
            }
        }

        return null;
    }
}

enum SpotSelectionType{
    FIRST_AVAILABLE
}

class ParkingSpotService{

    Map<Integer, ParkingSpot> spotMap;
    Map<SpotSelectionType, ParkingSpotSelectionStrategy> strategyMap;

    static ParkingSpotService instance = new ParkingSpotService();

    private ParkingSpotService(){

        spotMap = new ConcurrentHashMap<>();

        strategyMap = new HashMap<>();
        strategyMap.put(SpotSelectionType.FIRST_AVAILABLE, new FirstAvailableStrategy());
    }

    void addParkingSpot(ParkingSpot spot){
        spotMap.put(spot.id, spot);
    }

    ParkingSpot findSpot(VehicleType type, SpotSelectionType selectionType){

        ParkingSpotSelectionStrategy strategy = strategyMap.get(selectionType);

        return strategy.selectSpot(new ArrayList<>(spotMap.values()), type);
    }

    void freeSpot(int spotId){
        spotMap.get(spotId).free();
    }

    Map<Integer, Map<SpotType,Integer>> getAvailabilityPerFloor(){

        Map<Integer, Map<SpotType,Integer>> result = new HashMap<>();

        for(ParkingSpot spot : spotMap.values()){

            if(!spot.isAvailable) continue;

            result.putIfAbsent(spot.floorNo, new HashMap<>());

            Map<SpotType,Integer> floorMap = result.get(spot.floorNo);

            floorMap.put(spot.type,
                    floorMap.getOrDefault(spot.type,0)+1);
        }

        return result;
    }
}

enum TicketStatus{
    ACTIVE, PAID
}

class ParkingTicket{

    String id;
    Vehicle vehicle;
    int spotId;
    int floorNo;
    LocalDateTime entryTime;
    LocalDateTime exitTime;
    TicketStatus status;

    ParkingTicket(String id, Vehicle vehicle, ParkingSpot spot){

        this.id = id;
        this.vehicle = vehicle;
        this.spotId = spot.id;
        this.floorNo = spot.floorNo;

        this.entryTime = LocalDateTime.now();
        this.status = TicketStatus.ACTIVE;
    }
}

interface PriceCalculator{
    double calculatePrice(ParkingTicket ticket);
}

class HourlyPriceCalculator implements PriceCalculator{

    int ratePerHour = 10;

    public double calculatePrice(ParkingTicket ticket){

        long minutes = Duration.between(ticket.entryTime, LocalDateTime.now()).toMinutes();

        long hours = (long)Math.ceil(minutes/60.0);

        return hours * ratePerHour;
    }
}

class TicketService{

    Map<String, ParkingTicket> ticketMap;

    PriceCalculator priceCalculator;

    static TicketService instance = new TicketService();

    private TicketService(){

        ticketMap = new ConcurrentHashMap<>();
        priceCalculator = new HourlyPriceCalculator();
    }

    ParkingTicket createTicket(Vehicle vehicle, ParkingSpot spot){

        String id = UUID.randomUUID().toString();

        ParkingTicket ticket = new ParkingTicket(id, vehicle, spot);

        ticketMap.put(id, ticket);

        return ticket;
    }

    double closeTicket(String ticketId){

        ParkingTicket ticket = ticketMap.get(ticketId);

        ticket.exitTime = LocalDateTime.now();

        double price = priceCalculator.calculatePrice(ticket);

        ticket.status = TicketStatus.PAID;

        ParkingSpotService.instance.freeSpot(ticket.spotId);

        return price;
    }
}

interface PaymentProcessor{
    boolean pay(double amount);
}

class UpiPaymentProcessor implements PaymentProcessor{

    public boolean pay(double amount){

        System.out.println("UPI payment successful: " + amount);

        return true;
    }
}

class CashPaymentProcessor implements PaymentProcessor{

    public boolean pay(double amount){

        System.out.println("Cash payment successful: " + amount);

        return true;
    }
}

enum PaymentType{
    UPI, CASH
}

class PaymentService{

    Map<PaymentType, PaymentProcessor> processorMap;

    static PaymentService instance = new PaymentService();

    private PaymentService(){

        processorMap = Map.of(
                PaymentType.UPI, new UpiPaymentProcessor(),
                PaymentType.CASH, new CashPaymentProcessor()
        );
    }

    boolean processPayment(double amount, PaymentType type){

        return processorMap.get(type).pay(amount);
    }
}

class EntryGate{

    ParkingSpotService spotService = ParkingSpotService.instance;
    TicketService ticketService = TicketService.instance;

    ParkingTicket parkVehicle(Vehicle vehicle){

        ParkingSpot spot = spotService.findSpot(vehicle.type,
                SpotSelectionType.FIRST_AVAILABLE);

        if(spot == null){
            throw new RuntimeException("Parking Full");
        }

        return ticketService.createTicket(vehicle, spot);
    }
}

class ExitGate{

    TicketService ticketService = TicketService.instance;
    PaymentService paymentService = PaymentService.instance;

    void exitVehicle(String ticketId, PaymentType paymentType){

        double amount = ticketService.closeTicket(ticketId);

        paymentService.processPayment(amount, paymentType);
    }
}

public class ParkingLotApp {

    public static void main(String[] args) {

        ParkingSpotService.instance.addParkingSpot(
                new ParkingSpot(1,1,SpotType.CAR));

        ParkingSpotService.instance.addParkingSpot(
                new ParkingSpot(2,1,SpotType.CAR));

        ParkingSpotService.instance.addParkingSpot(
                new ParkingSpot(3,1,SpotType.BIKE));

        EntryGate entryGate = new EntryGate();
        ExitGate exitGate = new ExitGate();

        Vehicle car = new Vehicle("KA01AB1234", VehicleType.CAR);

        ParkingTicket ticket = entryGate.parkVehicle(car);

        exitGate.exitVehicle(ticket.id, PaymentType.UPI);
    }
}
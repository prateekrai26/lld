/*

Design Low Level Design of Uber Application

Features to implement
User can search for Ride from To Location, User can cancel the ride after Ride is created.
Driver can accept Ride and start the ride , end the ride .
Flow Diagram ->
User -> search Ride -> Driver Accept the ride -> Driver starts the ride ->  Driver reached the location -> Driver Accept the payment ->
Driver ends the ride

Entities
User , Ride , Driver  , Location , Payment , Vehicle


 */

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

class User {
    String id;
    String name;
    String phone;
    Location currentLocation;
}
class Rider extends User{
    List<Ride> pastRide;
}
class Driver extends User{
    Vehicle vehicle;
    boolean isActive;

    boolean isActive(){
        return this.isActive;
    }
}


interface RideState{
    RideStatus getRideStatus();
    void acceptRide(Ride ride);
    void cancelRide(Ride ride);
    void startRide(Ride ride);
    void completeRide(Ride ride);
}

class RequestedState implements RideState{

    @Override
    public RideStatus getRideStatus() {
        return RideStatus.REQUESTED;
    }

    @Override
    public void acceptRide(Ride ride) {
        System.out.println("Ride accepted");
        ride.setRideState(new AcceptedState());
    }

    @Override
    public void cancelRide(Ride ride) {
        System.out.println("Ride cancelled");
        ride.setRideState(new CancelledState());
    }

    @Override
    public void startRide(Ride ride) {
        throw new IllegalStateException("Ride cannot be started before accepting ride");
    }

    @Override
    public void completeRide(Ride ride) {
        throw new IllegalStateException("Ride not accepted yet");
    }
}

class AcceptedState implements RideState{
    @Override
    public RideStatus getRideStatus() {
        return RideStatus.ACCEPTED;
    }

    @Override
    public void acceptRide(Ride ride) {
        throw new IllegalStateException("Ride already accepted");
    }

    @Override
    public void cancelRide(Ride ride) {
        System.out.println("Ride cancelled");
        ride.setRideState(new CancelledState());
    }

    @Override
    public void startRide(Ride ride) {
        System.out.println("Ride Started");
        ride.setRideState(new StartState());
    }

    @Override
    public void completeRide(Ride ride) {
        throw new IllegalStateException("Ride not  started yet ");
    }
}

class StartState implements RideState{
    @Override
    public RideStatus getRideStatus() {
       return RideStatus.STARTED;
    }

    @Override
    public void acceptRide(Ride ride) {
        throw new IllegalStateException("Ride is already started");
    }

    @Override
    public void cancelRide(Ride ride) {
        throw new IllegalStateException("Cannot cancel after ride started");
    }

    @Override
    public void startRide(Ride ride) {
        throw new IllegalStateException("Ride already started");
    }

    @Override
    public void completeRide(Ride ride) {

    }
}

class CancelledState implements RideState{
    @Override
    public RideStatus getRideStatus() {
        return RideStatus.CANCELLED;
    }

    @Override
    public void acceptRide(Ride ride) {
        throw new UnsupportedOperationException("Cancelled Ride cannot be accepted ");
    }

    @Override
    public void cancelRide(Ride ride) {
        throw new IllegalStateException("Ride cancelled");
    }

    @Override
    public void startRide(Ride ride) {
        throw new IllegalStateException("Ride cancelled");
    }

    @Override
    public void completeRide(Ride ride) {
        throw new IllegalStateException("Ride cancelled");
    }
}

class CompletedState implements RideState{
    @Override
    public RideStatus getRideStatus() {
        return RideStatus.COMPLETED;
    }

    @Override
    public void acceptRide(Ride ride) {
        throw new IllegalStateException("Ride already completed");
    }

    @Override
    public void cancelRide(Ride ride) {
        throw new IllegalStateException("Ride already completed");
    }

    @Override
    public void startRide(Ride ride) {
        throw new IllegalStateException("Ride already completed");
    }

    @Override
    public void completeRide(Ride ride) {
        throw new IllegalStateException("Ride already completed");
    }
}

class Ride{
    String id;
    String userId;
    String driverId;
    Location source;
    RideState rideState;
    public Ride(String userId, String driverId, Location source, Location destination, double fareAmount, VehicleCategory rideCategory) {
        this.userId = userId;
        this.driverId = driverId;
        this.source = source;
        this.destination = destination;
        this.fareAmount = fareAmount;
        this.rideCategory = rideCategory;
        this.rideState = new RequestedState();
    }

    void setRideState(RideState rideState) {
        this.rideState = rideState;
    }

    Location destination;
    double fareAmount;
    VehicleCategory rideCategory;
}
  enum RideStatus{
     REQUESTED,
      ACCEPTED,
      STARTED,
      COMPLETED,
      CANCELLED
  }

class Vehicle{
    String vehicleId;
    String vehicleNo;
    VehicleCategory vehicleCategory;
}

enum VehicleCategory{
    GO,PREMIER,XL
}


class Location{
    long latitude;
    long longitude;
}


class RiderService{
    List<Rider> riderList;

    static RiderService riderService = new RiderService();

    public static RiderService getInstance(){
        return riderService;
    }

    private RiderService(){}

    public void addRider(Rider rider) {
        riderList.add(rider);
    }
}




class DriverService{
    List<Driver> driverList = new ArrayList<>();

    static DriverService driverService = new DriverService();

    public static DriverService getInstance(){
        return driverService;
    }

    private DriverService(){}

    public void addDriver(Driver driver) {
        driverList.add(driver);
    }

    public List<Driver> getAvailableDrivers() {
        return driverList.stream()
                .filter(Driver::isActive)
                .toList();
    }
    public List<Driver> getAvailableDriversByVehicleCategory(VehicleCategory vehicleCategory) {
        return driverList.stream()
                .filter(Driver::isActive)
                .filter(x -> x.vehicle.vehicleCategory == vehicleCategory)
                .toList();
    }

}

interface FareCalculator{
    double calculateFare(Location startLocation, Location endLocation);
}

class PerKMDistanceBasedFareCalculator implements  FareCalculator{
    double perKmFare = 10.0;

    @Override
    public double calculateFare(Location startLocation, Location endLocation) {
       double distance = Math.sqrt(Math.pow(startLocation.latitude - endLocation.latitude,2)  +
               Math.pow(startLocation.longitude - endLocation.longitude,2));
       return distance * perKmFare;
    }
}

class FixedFareCalculator implements  FareCalculator{
    double perKmFare = 10.0;

    @Override
    public double calculateFare(Location startLocation, Location endLocation) {
         return perKmFare;
    }
}

abstract class FareDecorator{
    FareCalculator fareCalculator;

    public FareDecorator(FareCalculator fareCalculator){
        this.fareCalculator = fareCalculator;
    }

    public abstract double calculateFare(Location startLocation, Location endLocation);
}

class SurchargeFareDecorator extends  FareDecorator{

    public SurchargeFareDecorator(FareCalculator fareCalculator) {
        super(fareCalculator);
    }

    public double calculateFare(Location startLocation, Location endLocation) {
        double baseFare = fareCalculator.calculateFare(startLocation, endLocation);
        double surcharge = 1.2; // 20% surcharge
        return baseFare * surcharge;
    }
}

interface DriverMatchingStrategy{
    Driver matchDriver(Location sourceLocation , VehicleCategory vehicleCategory);
}

class NearByLocationBasedDriverMatching implements DriverMatchingStrategy{
    DriverService driverService = DriverService.getInstance();

    @Override
    public Driver matchDriver(Location sourceLocation ,  VehicleCategory vehicleCategory) {
       Driver driver = null;
       double minDistance = Double.MAX_VALUE;
       for(Driver x  : driverService.getAvailableDriversByVehicleCategory(vehicleCategory)){
           double distance = Math.sqrt(Math.pow(x.currentLocation.latitude - sourceLocation.latitude,2))  +
                   Math.sqrt(Math.pow(x.currentLocation.longitude - sourceLocation.longitude,2));
           if(distance < minDistance){
               minDistance = distance;
               driver = x;
           }
       }
       return driver;
    }
}

class RideService{
    List<Ride> rides;
    static RideService riderService = new RideService();
    DriverMatchingStrategy driverMatchingStrategy = new NearByLocationBasedDriverMatching();
    public static RideService getInstance(){
        return riderService;
    }

    void setDriverMatchingStrategy(DriverMatchingStrategy driverMatchingStrategy){
        this.driverMatchingStrategy = driverMatchingStrategy;
    }

    void acceptRide(String driverId , String rideId){
        Ride ride = rides.stream().filter(x -> x.id.equals(rideId)).findFirst().orElse(null);
        System.out.println(ride.rideState.getRideStatus());
        if(ride.driverId.equals(driverId)){
            ride.rideState.acceptRide(ride);
            System.out.println(ride.rideState.getRideStatus());
        }

    }

    void cancelRide(String id , String rideId){
        Ride ride = rides.stream().filter(x -> x.id.equals(rideId)).findFirst().orElse(null);
        System.out.println(ride.rideState.getRideStatus());
        if(ride.driverId.equals(id) || ride.userId.equals(id)){
            ride.rideState.cancelRide(ride);
            System.out.println(ride.rideState.getRideStatus());
        }

    }

    void  startRide(String driverId , String rideId){
        Ride ride = rides.stream().filter(x -> x.id.equals(rideId)).findFirst().orElse(null);
        System.out.println(ride.rideState.getRideStatus());
        if(ride.driverId.equals(driverId)){
            ride.rideState.startRide(ride);
            System.out.println(ride.rideState.getRideStatus());
        }

    }

    void completeRide(String driverId , String rideId){
        Ride ride = rides.stream().filter(x -> x.id.equals(rideId)).findFirst().orElse(null);
        System.out.println(ride.rideState.getRideStatus());
        if(ride.driverId.equals(driverId)){
            ride.rideState.completeRide(ride);
            System.out.println(ride.rideState.getRideStatus());
        }

    }


    private RideService(){}

    public Ride searchRide(String userId , Location source, Location destination ,VehicleCategory vehicleCategory){
        Driver driver = driverMatchingStrategy.matchDriver(source , vehicleCategory);
        double fare =  new SurchargeFareDecorator(new PerKMDistanceBasedFareCalculator()).calculateFare(source , destination);
        Ride ride = new Ride(userId , driver.id , source ,destination   , fare , vehicleCategory);
        rides.add(ride);
         return ride;
    }

}

public class Uber{


    public static void main(String[] args) {

    }
}
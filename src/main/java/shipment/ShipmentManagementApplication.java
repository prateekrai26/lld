package shipment;

/*
-------------------------------------------------------------
Shipment Management System - LLD
Using State Design Pattern for shipment lifecycle
-------------------------------------------------------------

ENTITIES
--------

Customer
DeliveryPartner
Shipment
TrackingEvent

SERVICES
--------

ShipmentService
NotificationService

STATE PATTERN

ShipmentState
CreatedState
AssignedState
PickedUpState
InTransitState
OutForDeliveryState
DeliveredState

DATA FLOW
---------

1 Customer creates shipment
2 Shipment starts in CreatedState
3 assignDeliveryPartner -> AssignedState
4 pickup -> PickedUpState
5 transit -> InTransitState
6 out for delivery -> OutForDeliveryState
7 delivered -> DeliveredState

Each transition
 -> TrackingEvent added
 -> Customer notified
-------------------------------------------------------------
*/

import java.util.*;


/* ---------------------------------------------------------
MAIN APPLICATION
--------------------------------------------------------- */

public class ShipmentManagementApplication {

    public static void main(String[] args) {

        ShipmentSystem shipmentSystem = new ShipmentSystem();

        Customer customer = new Customer("C1", "Prateek", "999999999");

        Shipment shipment = shipmentSystem.createShipment(
                "S1",
                customer,
                "Warehouse Bangalore",
                "Electronic City");

        shipmentSystem.partnerPickedUpShipment(shipment);

        shipmentSystem.updateShipmentState(shipment);
        shipmentSystem.updateShipmentState(shipment);
        shipmentSystem.updateShipmentState(shipment);

        System.out.println("Tracking History:");

        for (TrackingEvent event : shipmentSystem.trackShipment(shipment)) {
            System.out.println(event);
        }
    }
}


/* ---------------------------------------------------------
STATE INTERFACE
--------------------------------------------------------- */

interface ShipmentState {

    void next(Shipment shipment, NotificationService notificationService);

    String getStatus();
}


/* ---------------------------------------------------------
STATE CLASSES
--------------------------------------------------------- */

class CreatedState implements ShipmentState {

    public void next(Shipment shipment, NotificationService notificationService) {

        shipment.setState(new AssignedState());

        shipment.addTracking("Shipment assigned");

        notificationService.notifyCustomer(
                shipment.customer,
                "Shipment assigned to delivery partner");
    }

    public String getStatus() {
        return "CREATED";
    }
}


class AssignedState implements ShipmentState {

    public void next(Shipment shipment, NotificationService notificationService) {

        shipment.setState(new PickedUpState());

        shipment.addTracking("Shipment picked up");

        notificationService.notifyCustomer(
                shipment.customer,
                "Shipment picked up");
    }

    public String getStatus() {
        return "ASSIGNED";
    }
}


class PickedUpState implements ShipmentState {

    public void next(Shipment shipment, NotificationService notificationService) {

        shipment.setState(new InTransitState());

        shipment.addTracking("Shipment in transit");

        notificationService.notifyCustomer(
                shipment.customer,
                "Shipment in transit");
    }

    public String getStatus() {
        return "PICKED_UP";
    }
}


class InTransitState implements ShipmentState {

    public void next(Shipment shipment, NotificationService notificationService) {

        shipment.setState(new OutForDeliveryState());

        shipment.addTracking("Shipment out for delivery");

        notificationService.notifyCustomer(
                shipment.customer,
                "Shipment out for delivery");
    }

    public String getStatus() {
        return "IN_TRANSIT";
    }
}


class OutForDeliveryState implements ShipmentState {

    public void next(Shipment shipment, NotificationService notificationService) {

        shipment.setState(new DeliveredState());

        shipment.addTracking("Shipment delivered");

        notificationService.notifyCustomer(
                shipment.customer,
                "Shipment delivered");
    }

    public String getStatus() {
        return "OUT_FOR_DELIVERY";
    }
}


class DeliveredState implements ShipmentState {

    public void next(Shipment shipment, NotificationService notificationService) {

        System.out.println("Shipment already delivered");
    }

    public String getStatus() {
        return "DELIVERED";
    }
}


/* ---------------------------------------------------------
ENTITY CLASSES
--------------------------------------------------------- */

class Customer {

    String customerId;
    String name;
    String phone;

    Customer(String customerId, String name, String phone) {

        this.customerId = customerId;
        this.name = name;
        this.phone = phone;
    }
}


class DeliveryPartner {

    String partnerId;
    String name;
    boolean available = true;

    DeliveryPartner(String partnerId, String name) {

        this.partnerId = partnerId;
        this.name = name;
    }
}


class Shipment {

    String shipmentId;

    Customer customer;

    String pickupLocation;
    String dropLocation;

    DeliveryPartner deliveryPartner;

    ShipmentState state;

    List<TrackingEvent> trackingEvents = new ArrayList<>();


    Shipment(
            String shipmentId,
            Customer customer,
            String pickupLocation,
            String dropLocation) {

        this.shipmentId = shipmentId;
        this.customer = customer;
        this.pickupLocation = pickupLocation;
        this.dropLocation = dropLocation;

        this.state = new CreatedState();
    }

    void setState(ShipmentState state) {

        this.state = state;
    }

    void moveNextState(NotificationService notificationService) {

        state.next(this, notificationService);
    }

    void addTracking(String message) {

        trackingEvents.add(
                new TrackingEvent(message, state.getStatus()));
    }
}


class TrackingEvent {

    String location;
    String status;
    long timestamp;

    TrackingEvent(String location, String status) {

        this.location = location;
        this.status = status;
        this.timestamp = System.currentTimeMillis();
    }

    public String toString() {

        return timestamp + " : " + status + " -> " + location;
    }
}


/* ---------------------------------------------------------
SERVICES
--------------------------------------------------------- */

class ShipmentService {

    Map<String, Shipment> shipments = new HashMap<>();


    Shipment createShipment(
            String shipmentId,
            Customer customer,
            String pickup,
            String drop) {

        Shipment shipment =
                new Shipment(shipmentId, customer, pickup, drop);

        shipments.put(shipmentId, shipment);

        shipment.addTracking("Shipment created");

        // Automatically assign a delivery partner
        DeliveryPartner partner = new DeliveryPartner("AUTO_D1", "AutoAssignedPartner");
        shipment.deliveryPartner = partner;

        shipment.addTracking("Delivery partner automatically assigned");

        return shipment;
    }


    void assignDeliveryPartner(
            Shipment shipment,
            DeliveryPartner partner) {

        shipment.deliveryPartner = partner;
    }
}


class NotificationService {

    void notifyCustomer(Customer customer, String message) {

        System.out.println(
                "Notification to "
                        + customer.name
                        + " : "
                        + message);
    }
}
// ---------------------------------------------------------
// FACADE CLASS
// ---------------------------------------------------------
class ShipmentSystem {

    ShipmentService shipmentService = new ShipmentService();
    NotificationService notificationService = new NotificationService();

    Shipment createShipment(
            String shipmentId,
            Customer customer,
            String pickup,
            String drop) {

        return shipmentService.createShipment(
                shipmentId,
                customer,
                pickup,
                drop);
    }

    void assignDeliveryPartner(
            Shipment shipment,
            DeliveryPartner partner) {

        shipmentService.assignDeliveryPartner(shipment, partner);
    }

    void partnerPickedUpShipment(Shipment shipment) {

        // partner triggers first transition (pickup)
        shipment.moveNextState(notificationService);
    }

    void updateShipmentState(Shipment shipment) {

        shipment.moveNextState(notificationService);
    }

    List<TrackingEvent> trackShipment(Shipment shipment) {

        return shipment.trackingEvents;
    }
}
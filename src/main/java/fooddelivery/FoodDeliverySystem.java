package fooddelivery;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/*

Problem Statement

Design a Food Delivery System like Swiggy or Zomato where users can browse restaurants,
order food, and get it delivered.

The system should allow:
    • Users to view restaurants and menus
    • Users to place orders
    • Restaurants to manage their menu
    • Delivery partners to deliver orders
    • Users to track order status

------------------------------------------------------------------------------------

Entities

User
 ├── Customer
 └── DeliveryPartner

Restaurant
FoodItem
Cart
Order
Location

------------------------------------------------------------------------------------

Service Layer

UserService
RestaurantService
DeliveryPartnerService
OrderService

------------------------------------------------------------------------------------

Data Flow

App -> UserService -> create user

App -> RestaurantService -> search restaurants / view menus

App -> Customer -> add food to cart

App -> OrderService -> place order

OrderService
    -> calculates final price
    -> processes payment
    -> assigns delivery partner

DeliveryPartnerService
    -> matches driver based on strategy

OrderService
    -> updates order status
    -> notifies observers (customer)

------------------------------------------------------------------------------------

Design Patterns Used

Strategy Pattern
    - DriverMatchingStrategy
    - PaymentStrategy

Decorator Pattern
    - PriceCalculator
    - CouponDecorator

Observer Pattern
    - OrderObserver
    - CustomerNotifier

------------------------------------------------------------------------------------

Simplifications for Interview

• Driver matching uses first available partner
• Payment is mocked
• Restaurant search simplified
• Inventory management simplified
• Single restaurant per order

------------------------------------------------------------------------------------

*/


class Location {

    double latitude;
    double longitude;

    Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}


enum UserStatus {
    ACTIVE,
    INACTIVE
}


class User {

    String userId;
    String name;
    Location currentLocation;
    UserStatus status;

    User(String userId, String name, Location location) {
        this.userId = userId;
        this.name = name;
        this.currentLocation = location;
        this.status = UserStatus.ACTIVE;
    }
}


class Customer extends User {

    Cart cart;

    Customer(String userId, String name, Location location) {
        super(userId, name, location);
        cart = new Cart();
    }
}


class DeliveryPartner extends User {

    boolean isAvailable = true;

    DeliveryPartner(String userId, String name, Location location) {
        super(userId, name, location);
    }

    void setAvailable(boolean available) {
        isAvailable = available;
    }
}


class FoodItem {

    String id;
    String name;
    double price;

    FoodItem(String id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }
}


class Cart {

    String restaurantId;

    List<FoodItem> foodItemList = new ArrayList<>();

    void addFood(FoodItem item) {
        foodItemList.add(item);
    }

    double getTotalPrice() {

        double total = 0;

        for (FoodItem f : foodItemList) {
            total += f.price;
        }

        return total;
    }
}


class Restaurant {

    String restaurantId;
    String name;
    Location location;

    Map<String, FoodItem> menu = new ConcurrentHashMap<>();

    Restaurant(String id, String name, Location location) {
        this.restaurantId = id;
        this.name = name;
        this.location = location;
    }

    void addFoodItem(FoodItem item) {
        menu.put(item.id, item);
    }
}


class RestaurantService {

    Map<String, Restaurant> restaurantMap = new ConcurrentHashMap<>();

    void registerRestaurant(Restaurant restaurant) {
        restaurantMap.put(restaurant.restaurantId, restaurant);
    }

    List<Restaurant> searchRestaurantByFood(String foodName) {

        List<Restaurant> result = new ArrayList<>();

        for (Restaurant r : restaurantMap.values()) {

            for (FoodItem item : r.menu.values()) {

                if (item.name.equalsIgnoreCase(foodName)) {
                    result.add(r);
                }
            }
        }

        return result;
    }
}


enum OrderStatus {

    PLACED,
    PREPARING,
    OUT_FOR_DELIVERY,
    DELIVERED
}


interface OrderObserver {

    void update(Order order);
}


class CustomerNotifier implements OrderObserver {

    public void update(Order order) {

        System.out.println(
                "Order "
                        + order.orderId
                        + " status updated to "
                        + order.status
        );
    }
}


class Order {

    String orderId;
    String customerId;
    String restaurantId;
    String deliveryPartnerId;

    List<FoodItem> items;

    double totalPrice;

    OrderStatus status;

    List<OrderObserver> observers = new ArrayList<>();

    void addObserver(OrderObserver observer) {
        observers.add(observer);
    }

    void notifyObservers() {

        for (OrderObserver observer : observers) {
            observer.update(this);
        }
    }
}


interface DriverMatchingStrategy {

    DeliveryPartner match(Location restaurantLocation,
                          List<DeliveryPartner> partners);
}


class NearestDriverStrategy implements DriverMatchingStrategy {

    public DeliveryPartner match(Location restaurantLocation,
                                 List<DeliveryPartner> partners) {

        for (DeliveryPartner p : partners) {

            if (p.isAvailable) {
                return p;
            }
        }

        return null;
    }
}


class DeliveryPartnerService {

    Map<String, DeliveryPartner> partners = new ConcurrentHashMap<>();

    DriverMatchingStrategy strategy = new NearestDriverStrategy();

    void registerPartner(DeliveryPartner partner) {
        partners.put(partner.userId, partner);
    }

    DeliveryPartner assignDriver(Location location) {

        return strategy.match(
                location,
                new ArrayList<>(partners.values())
        );
    }
}


interface PaymentStrategy {

    boolean pay(double amount);
}


class UpiPayment implements PaymentStrategy {

    public boolean pay(double amount) {

        System.out.println("UPI payment: " + amount);
        return true;
    }
}


class CardPayment implements PaymentStrategy {

    public boolean pay(double amount) {

        System.out.println("Card payment: " + amount);
        return true;
    }
}


interface PriceCalculator {

    double calculate();
}


class BasePrice implements PriceCalculator {

    Cart cart;

    BasePrice(Cart cart) {
        this.cart = cart;
    }

    public double calculate() {
        return cart.getTotalPrice();
    }
}


abstract class PriceDecorator implements PriceCalculator {

    PriceCalculator calculator;

    PriceDecorator(PriceCalculator calculator) {
        this.calculator = calculator;
    }
}


class CouponDecorator extends PriceDecorator {

    double discount;

    CouponDecorator(PriceCalculator calculator, double discount) {
        super(calculator);
        this.discount = discount;
    }

    public double calculate() {

        return calculator.calculate() - discount;
    }
}


class OrderService {

    Map<String, Order> orders = new ConcurrentHashMap<>();

    DeliveryPartnerService deliveryService;

    OrderService(DeliveryPartnerService service) {
        this.deliveryService = service;
    }

    String placeOrder(Customer customer,
                      PaymentStrategy paymentStrategy) {

        Cart cart = customer.cart;

        PriceCalculator calculator =
                new BasePrice(cart);

        double finalPrice = calculator.calculate();

        paymentStrategy.pay(finalPrice);

        Order order = new Order();

        order.orderId = UUID.randomUUID().toString();
        order.customerId = customer.userId;
        order.restaurantId = cart.restaurantId;
        order.items = cart.foodItemList;
        order.totalPrice = finalPrice;
        order.status = OrderStatus.PLACED;

        DeliveryPartner partner =
                deliveryService.assignDriver(customer.currentLocation);

        if (partner != null) {

            order.deliveryPartnerId = partner.userId;
            partner.setAvailable(false);
        }

        order.addObserver(new CustomerNotifier());

        orders.put(order.orderId, order);

        return order.orderId;
    }

    void updateOrderStatus(String orderId,
                           OrderStatus status) {

        Order order = orders.get(orderId);

        order.status = status;

        order.notifyObservers();

        if (status == OrderStatus.DELIVERED) {

            deliveryService.partners
                    .get(order.deliveryPartnerId)
                    .setAvailable(true);
        }
    }
}


public class FoodDeliverySystem {

    public static void main(String[] args) {

        DeliveryPartnerService deliveryService =
                new DeliveryPartnerService();

        OrderService orderService =
                new OrderService(deliveryService);

        DeliveryPartner partner =
                new DeliveryPartner(
                        "D1",
                        "Ravi",
                        new Location(12.9, 77.5)
                );

        deliveryService.registerPartner(partner);

        Customer customer =
                new Customer(
                        "C1",
                        "Prateek",
                        new Location(12.9, 77.6)
                );

        FoodItem pizza =
                new FoodItem("F1", "Pizza", 200);

        customer.cart.restaurantId = "R1";
        customer.cart.addFood(pizza);

        PaymentStrategy payment = new UpiPayment();

        String orderId =
                orderService.placeOrder(customer, payment);

        System.out.println("Order placed: " + orderId);

        orderService.updateOrderStatus(
                orderId,
                OrderStatus.OUT_FOR_DELIVERY
        );
    }
}
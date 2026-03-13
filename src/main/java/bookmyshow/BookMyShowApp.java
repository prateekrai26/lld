package bookmyshow;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/*
    BOOKMYSHOW LLD

    FLOW

    User -> Select City
         -> List Movies
         -> Select Movie
         -> List Shows
         -> Select Seats
         -> Seat Lock
         -> Payment
         -> Booking Confirmed

    ENTITY RELATIONSHIP

    City
      ↓
    Theatre
      ↓
    Screen
      ↓
    Show
      ↓
    Seats

    KEY DESIGN DECISIONS

    1. Seats belong to Screen (layout)
    2. Each Show maintains its own seatMap for seat availability
    3. Seat level locking used for high concurrency
    4. ReentrantLock.tryLock() used to avoid blocking
    5. Seats sorted before locking to avoid deadlocks
*/


/* ================= USER ================= */

class User {

    String id;
    String name;
    String phone;

    City city;

    User(String id,String name,String phone,City city){
        this.id=id;
        this.name=name;
        this.phone=phone;
        this.city=city;
    }
}


/* ================= CITY ================= */

class City {

    String name;

    City(String name){
        this.name=name;
    }
}


/* ================= MOVIE ================= */

class Movie {

    String id;
    String name;

    int duration;

    Movie(String id,String name,int duration){
        this.id=id;
        this.name=name;
        this.duration=duration;
    }
}


/* ================= THEATRE ================= */

class Theatre {

    String id;
    String name;

    City city;

    // one theatre contains multiple screens
    List<Screen> screens = new ArrayList<>();

    Theatre(String id,String name,City city){
        this.id=id;
        this.name=name;
        this.city=city;
    }
}


/* ================= SCREEN ================= */

class Screen {

    String id;
    String theatreId;

    // seat layout of the screen
    List<Seat> seats = new ArrayList<>();

    Screen(String id,String theatreId){
        this.id=id;
        this.theatreId=theatreId;
    }
}


/* ================= SEAT ================= */

enum SeatType{
    STANDARD,RECLINER
}

enum SeatStatus{
    AVAILABLE,LOCKED,BOOKED
}

class Seat {

    String id;

    SeatType type;

    // current seat state
    SeatStatus status = SeatStatus.AVAILABLE;

    // user who locked the seat
    String lockedBy;

    // lock expiry time (used in real systems for seat hold timeout)
    long lockExpiry;

    // seat level lock for concurrency control
    ReentrantLock lock = new ReentrantLock();

    Seat(String id,SeatType type){
        this.id=id;
        this.type=type;
    }
}


/* ================= SHOW ================= */

class Show {

    String id;

    String movieId;
    String screenId;

    LocalDateTime startTime;
    LocalDateTime endTime;

    /*
       Each show maintains its own seat map because
       seat availability changes per show.
    */
    Map<String,Seat> seatMap = new ConcurrentHashMap<>();

    Show(String id,String movieId,String screenId,
         LocalDateTime start,LocalDateTime end){

        this.id=id;
        this.movieId=movieId;
        this.screenId=screenId;
        this.startTime=start;
        this.endTime=end;
    }
}


/* ================= BOOKING ================= */

enum BookingStatus{
    CONFIRMED,FAILED
}

class Booking {

    String bookingId;

    String userId;
    String showId;

    List<String> seats;

    String paymentId;

    BookingStatus status;

    Booking(String bookingId,String userId,String showId,
            List<String> seats,String paymentId,BookingStatus status){

        this.bookingId=bookingId;
        this.userId=userId;
        this.showId=showId;
        this.seats=seats;
        this.paymentId=paymentId;
        this.status=status;
    }
}


/* ================= PAYMENT ================= */

enum PaymentMode{
    CARD,UPI
}

enum PaymentStatus{
    SUCCESS,FAILED
}

class Payment {

    String id;

    double amount;

    PaymentMode mode;
    PaymentStatus status;

    Payment(String id,double amount){
        this.id=id;
        this.amount=amount;
        this.mode=PaymentMode.UPI;
        this.status=PaymentStatus.SUCCESS;
    }
}


/* ================= PAYMENT SERVICE ================= */

class PaymentService {

    Payment pay(double amount){

        // simulate successful payment
        return new Payment(UUID.randomUUID().toString(),amount);
    }
}


/* ================= BOOKING SERVICE ================= */

class BookingService {

    Map<String,Booking> bookings = new HashMap<>();

    PaymentService paymentService = new PaymentService();

    // seat lock expiry time (5 minutes)
    static final long LOCK_TIME = 300000;


    Booking book(User user, Show show, List<String> seatIds){

        List<Seat> seats = new ArrayList<>();

        // fetch seat objects
        for(String id : seatIds){

            Seat seat = show.seatMap.get(id);

            if(seat == null)
                throw new RuntimeException("Seat not found");

            seats.add(seat);
        }

        /*
            IMPORTANT

            Sort seats before locking to avoid deadlocks.
            Ensures all threads acquire locks in same order.
        */
        seats.sort(Comparator.comparing(s -> s.id));

        List<Seat> lockedSeats = new ArrayList<>();

        try{

            /*
               Step 1: Try acquiring locks on all seats
               If any seat lock fails → fail fast
            */

            for(Seat seat : seats){

                boolean locked = seat.lock.tryLock();

                if(!locked){
                    throw new RuntimeException("Seat busy");
                }

                lockedSeats.add(seat);
            }


            /*
               Step 2: Validate seat availability
            */

            for(Seat seat : seats){

                if(seat.status != SeatStatus.AVAILABLE){
                    throw new RuntimeException("Seat unavailable");
                }
            }


            /*
               Step 3: Lock seats for the user
            */

            for(Seat seat : seats){

                seat.status = SeatStatus.LOCKED;
                seat.lockedBy = user.id;
                seat.lockExpiry = System.currentTimeMillis() + LOCK_TIME;
            }


            /*
               Step 4: Payment
            */

            Payment payment = paymentService.pay(100);


            /*
               Step 5: Confirm booking
            */

            for(Seat seat : seats){
                seat.status = SeatStatus.BOOKED;
            }


            String bookingId = UUID.randomUUID().toString();

            Booking booking = new Booking(
                    bookingId,
                    user.id,
                    show.id,
                    seatIds,
                    payment.id,
                    BookingStatus.CONFIRMED
            );

            bookings.put(bookingId,booking);

            return booking;

        } finally {

            /*
               Always release locks
            */

            for(Seat seat : lockedSeats){
                seat.lock.unlock();
            }
        }
    }
}


/* ================= APPLICATION ================= */

public class BookMyShowApp {

    public static void main(String[] args) {

        // create city
        City city = new City("Bangalore");

        // create movie
        Movie movie = new Movie("M1","Interstellar",180);

        // create theatre
        Theatre theatre = new Theatre("T1","PVR",city);

        // create screen
        Screen screen = new Screen("S1","T1");

        // create seat layout
        for(int i=1;i<=10;i++){
            screen.seats.add(new Seat("A"+i,SeatType.STANDARD));
        }

        theatre.screens.add(screen);

        // create show
        Show show = new Show(
                "SHOW1",
                movie.id,
                screen.id,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(3)
        );

        // initialize show seat availability
        for(Seat seat : screen.seats){
            show.seatMap.put(seat.id,new Seat(seat.id,seat.type));
        }

        // create user
        User user = new User("U1","Prateek","999999",city);

        // booking service
        BookingService bookingService = new BookingService();

        // book seats
        Booking booking = bookingService.book(
                user,
                show,
                Arrays.asList("A1","A2")
        );

        System.out.println("Booking confirmed: " + booking.bookingId);
    }
}
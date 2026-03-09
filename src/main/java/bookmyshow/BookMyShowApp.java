package bookmyshow;
/*

 User - Select City -> List all the movies from the city -> User Selects the movie ->  List all the available Movie Theateres ,  User Select available shows
-> user selects seats from available seats --- > make payment -> seat confirmed.

Entities - USer , City , Movie  , Theatre , SEAT , Show , Payment
 */


import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class User{
    String id;
    String name;
    String phone;
    City currentCity;

    public User(String id, String name, String phone, City currentCity) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.currentCity = currentCity;
    }
}

class UserService{
    Map<String , User> map = new HashMap<>();
    static UserService userService = new UserService();
    private UserService() {
        this.map = new HashMap<>();
    }
  static  UserService getUserServiceInstance() {
        return userService;
    }
    User getUser(String id) {
        return map.get(id);
    }
    void createUser(User user) {
        map.put(user.id, user);
    }
    void updateCity(String userId , City city) {
        User user = map.get(userId);
        user.currentCity = city;
    }
}

class City{
    String name;
    String state;
}

class Movie{
    String movieName;
    String id;
    long duration;
    List<City> cities = new ArrayList<>();
}

class MovieService{
    Map<String , Movie> movieMap = new HashMap<>();
    static MovieService movieService = new MovieService();
    private MovieService() { movieMap = new HashMap<>(); }
    Movie getMovie(String id) {
        return movieMap.get(id);
    }

    static MovieService  getMovieServiceInstance() {
        return movieService;
    }

    void createMovie(Movie movie) {
        movieMap.put(movie.movieName, movie);
    }

}

class Theatre{
    String id;
    String name;
    City city;
    HashSet<String> movies;

    public Theatre(String id, String name, City city) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.movies = new HashSet<>();
    }
}

class TheatreService{
    Map<String , Theatre> theatreMap = new HashMap<>();
    static TheatreService theatreService = new TheatreService();
    MovieService moviesService = MovieService.getMovieServiceInstance();
    Theatre getTheatre(String id) {
        return theatreMap.get(id);
    }
    static TheatreService  getTheatreServiceInstance() {
        return theatreService;
    }
    void createTheatre(Theatre theatre) {
        theatreMap.put(theatre.id, theatre);
    }
    List<Theatre> getTheatresByCity(City city) {
        List<Theatre> theatres = new ArrayList<>();
        for (Map.Entry<String , Theatre> entry : theatreMap.entrySet()) {
            Theatre theatre = entry.getValue();
            if (theatre.city.equals(city)) {
                theatres.add(theatre);
            }
        }
        return theatres;
    }
    List<Movie> getMoviesByCity(String cityName) {
        List<String> movieIds = new ArrayList<>();
        for (Map.Entry<String , Theatre> entry : theatreMap.entrySet()) {
            Theatre theatre = entry.getValue();
            if (theatre.city.equals(cityName)) {
                movieIds.addAll(theatre.movies);
            }
        }
        List<Movie> movies = new ArrayList<>();
        for(String movieId : movieIds) {
            Movie movie = moviesService.getMovie(movieId);
            movies.add(movie);
        }
        return movies;
    }

    List<Theatre> getTheatresByMovieId(String movieId) {
        List<Theatre> theatres = new ArrayList<>();
        for(Map.Entry<String , Theatre> entry : theatreMap.entrySet()) {
            Theatre theatre = entry.getValue();
           if(theatre.movies.contains(movieId)) {
               theatres.add(theatre);
           }
        }
        return theatres;
    }
}

class Show{
    String id;
    String theatreId;
    String movieId;
    LocalDateTime starTime;
    LocalDateTime endTime;
    Map<String , Seat> seatMap;

    public Show(String id, String theatreId, String movieId, LocalDateTime starTime, LocalDateTime endTime) {
        this.id = id;
        this.theatreId = theatreId;
        this.movieId = movieId;
        this.starTime = starTime;
        this.endTime = endTime;
        this.seatMap = new ConcurrentHashMap<>();
    }
}
class ShowService{
    Map<String , Show> showMap = new HashMap<>();
    static ShowService showService = new ShowService();
    BookingService bookingService = BookingService.getBookingServiceInstance() ;
    Show getShow(String id) {
        return showMap.get(id);
    }
    static ShowService  getShowServiceInstance() {
        return showService;
    }
    void createShow(Show show) {
        showMap.put(show.id, show);
    }
    List<Show> getShowsByTheatre(String theatreId) {
        List<Show> shows = new ArrayList<>();
        for (Map.Entry<String , Show> entry : showMap.entrySet()) {
            Show show = entry.getValue();
             if (show.theatreId.equals(theatreId)) {
                 shows.add(show);
             }
        }
        return shows;
    }
    Map<String, Seat> getSeatMap(String showID){
        Show show = showMap.get(showID);
        return show.seatMap;
    }

    Booking bookShowSeat(String showId , List<String> seatIds){
        Show show = showMap.get(showId);
        boolean isAllSeatsAvailable = true;
        if(show.seatMap.containsKey(show.id)){
            Seat seat = show.seatMap.get(show.id);
            if(!seat.isAvailable){
                throw new RuntimeException("Seats not available to book");
            }
        }
      Booking booking =  bookingService.book(show, seatIds);
        return booking;
    }
}

class Booking{
    String bookingId;
    String paymentId;
    List<String> seatIds;
    Movie movie;
    BookingStatus bookingStatus;

    public Booking(String bookingId, String paymentId, List<String> seatIds, Movie movie, BookingStatus bookingStatus) {
        this.bookingId = bookingId;
        this.paymentId = paymentId;
        this.seatIds = seatIds;
        this.movie = movie;
        this.bookingStatus = bookingStatus;
    }
}
enum BookingStatus{
    FAILED,CONFIRMED
}
class BookingService{
    Map<String ,Booking> bookings = new HashMap<>();
    static BookingService bookingService = new BookingService();
    PaymentService paymentService = PaymentService.getPaymentServiceInstance();
    MovieService movieService = MovieService.getMovieServiceInstance();
    private BookingService(){
        bookings = new HashMap<>();
    }
    static BookingService getBookingServiceInstance() {
        return bookingService;
    }
    Booking book(Show show , List<String> seatIds){
        String bookingId = UUID.randomUUID().toString();
        double amount = 100; // this can be calculated dynamically with strategy
        Payment payment = paymentService.payment(bookingId ,amount );
        return new Booking(bookingId , payment.paymentId , seatIds , movieService.getMovie(show.movieId) , BookingStatus.CONFIRMED);
    }
}
class Payment{
    public Payment(String paymentId, PaymentStatus paymentStatus, double totalPrice, PaymentMode paymentMode) {
        this.paymentId = paymentId;
        this.paymentStatus = paymentStatus;
        this.totalPrice = totalPrice;
        this.paymentMode = paymentMode;
    }

    String paymentId;
    PaymentStatus paymentStatus;
    double totalPrice;
    PaymentMode paymentMode;
}
enum PaymentMode{
    UPI,CARD
}
enum PaymentStatus{
    FAILED,SUCCESS
}
class PaymentService{
    Map<String , Payment> payments = new HashMap<>();
    static PaymentService paymentService = new PaymentService();
    private PaymentService(){

    }
    static PaymentService getPaymentServiceInstance() {
        return paymentService;
    }
    Payment payment(String bookingId , double totalPrice) {
        return new Payment(UUID.randomUUID().toString(), PaymentStatus.SUCCESS, totalPrice, PaymentMode.UPI);
    }
}

class Seat{
    String id;
    SeatType seatType;
    boolean isAvailable;
}

enum SeatType{
    STANDARD,RECLINER
}

class BookMyShowAppService{
    UserService userService = UserService.getUserServiceInstance();
    MovieService movieService = MovieService.getMovieServiceInstance();
    TheatreService theatreService = TheatreService.getTheatreServiceInstance();


}

class BookMyShowApp {
    public static void main(String[] args) {

    }
}
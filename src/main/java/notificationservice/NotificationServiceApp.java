package notificationservice;


import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class User{
    String userId;
    String name;
    String email;
    String mobileId;
    String phone;

    public User(String userId, String name, String email, String mobileId, String phone) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.mobileId = mobileId;
        this.phone = phone;
    }
}

interface NotificationProcessor{
   void sendNotification(Notification notification);
}
class EmailNotificationProcessor implements NotificationProcessor {
    public void sendNotification(Notification notification) {
        System.out.println("EMAIL Notification Sent for " +
                notification.user.userId + "with message " + notification.message );

    }
}

class PushNotificationProcessor implements NotificationProcessor  {

    public void sendNotification(Notification notification) {
        System.out.println("Push Notification Sent for " +
                notification.user.userId + "with message " + notification.message );
    }

}
class SMSNotificationProcessor implements NotificationProcessor  {
    @Override
    public void sendNotification(Notification notification) {
        System.out.println("SMS Notification Sent for " +
                notification.user.userId + "with message " + notification.message );

    }
}

enum NotificationType{
    SMS,EMAIL,PUSH
}


class NotificationProcessorFactory{

    NotificationProcessor getNotificationProcessor(NotificationType notificationType){
        if(notificationType.equals(NotificationType.EMAIL)) return new EmailNotificationProcessor();
        if(notificationType.equals(NotificationType.SMS)) return new SMSNotificationProcessor();
        if(notificationType.equals(NotificationType.PUSH)) return new PushNotificationProcessor();
        return null;
    }
}



class Notification{
    String notificationId;
    User user;
    String message;
    NotificationStatus notificationStatus;
    Instant timestamp;

    public Notification(User user, String message) {
        this.user = user;
        this.message = message;
        this.notificationId = UUID.randomUUID().toString();
        timestamp = Instant.now();
        notificationStatus = NotificationStatus.CREATED;
    }

}



enum  NotificationStatus{
    CREATED,SENT,FAILED
}

class NotificationService{
    Map<String , Notification> notifications = new ConcurrentHashMap<>();

    Map<String  , Set<NotificationProcessor>> notificationProcessorMap = new HashMap<>();

    NotificationProcessorFactory notificationProcessorFactory = new NotificationProcessorFactory();


    void subscribeToNotification(User user , NotificationType notificationType){
        notificationProcessorMap.computeIfAbsent(user.userId , x-> new HashSet<>())
                .add( notificationProcessorFactory.getNotificationProcessor(notificationType));
    }

    void sendMessage(User user , String message){
        Notification notification = new Notification(user , message);
        notifications.put(notification.notificationId , notification);
        for(NotificationProcessor notificationProcessor : notificationProcessorMap.get(user.userId)){
            notificationProcessor.sendNotification(notification);
        }
    }

}
public class NotificationServiceApp {

    public static void main(String[] args) {
        User user = new User("user1" , "Prateek" ,
                "email1" , "mobileId1" , "phone1");
        User user2 = new User("user2" , "Amit" ,
                "email1" , "mobileId1" , "phone1");
        NotificationService notificationService= new NotificationService();
        notificationService.subscribeToNotification(user , NotificationType.EMAIL);
        notificationService.subscribeToNotification(user , NotificationType.SMS);
        notificationService.subscribeToNotification(user2 , NotificationType.PUSH);
        notificationService.subscribeToNotification(user2 , NotificationType.EMAIL);

        notificationService.sendMessage(user , "Hello How Are you ");

    }
}

package chat;

/*
  User Can Create account, send message to friend ,create group , send message in the group , other folks gets notification for the message
  Design a Chat Application that allows users to communicate with each other through one-to-one chats and group chats. The system should
  support sending messages, receiving messages in real-time, and maintaining chat history

  Entities -> Account , Group , Chat

 */


import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class User {
    String name;
    String userId;

    public User(String name, String userId) {
        this.name = name;
        this.userId = userId;
    }
    String getName() {
        return name;
    }
    String getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Objects.equals(userId, user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }

}
enum MessageStatus {
    SENT,
    DELIVERED,
    READ
}

class UserService{
    Map<String , User> accounts = new ConcurrentHashMap<>();
    static UserService instance = new UserService();
    void addAccount(User user){
        accounts.put(user.userId, user);
    }

    static UserService getAccountServiceInstance(){
        return instance;
    }

    User getAccount(String accountId){
        return accounts.get(accountId);
    }
}

class Message{
    String messageId;
    User sender;
    String content;
    Instant timestamp;
    private MessageStatus status;
    public Message(User sender, String content) {
        this.sender = sender;
        this.content = content;
        this.timestamp = Instant.now();
        this.messageId = UUID.randomUUID().toString();
        status = MessageStatus.SENT;
    }

    String getContent() {
        return content;
    }
    public User getSender() {
        return sender;
    }

}

class Chat{
    String id;
    Set<User> participants;

    Chat(String id , Set<User> participants){
        this.id = id;
        this.participants = participants;
    }
    Set<User> getParticipants(){
        return participants;
    }

    String getId(){
        return id;
    }
}


class GroupChat extends Chat{
   String groupName;

   GroupChat(String chatId , String groupName , Set<User> participants){
       super(chatId , participants);
       this.groupName = groupName;
   }
}

class PrivateChat extends Chat{

    PrivateChat(String id, Set<User> participants) {
        super(id, participants);
        if (participants.size() != 2) {
            throw new IllegalArgumentException("Private chat must have exactly 2 users");
        }

    }
}


class ChatFactory{
    public Chat createPrivateChat(User u1, User u2) {

        Set<User> users = new HashSet<>();
        users.add(u1);
        users.add(u2);

        return new PrivateChat(UUID.randomUUID().toString(), users);
    }

    public Chat createGroupChat(String name, Set<User> users) {

        return new GroupChat(UUID.randomUUID().toString(), name, users);
    }
}


interface NotificationStrategy {

void send(User user, Message message);
}

class PushNotificationStrategy implements NotificationStrategy {

    public void send(User user, Message message) {

        System.out.println(
                "Push Notification to " +
                        user.getName() +
                        " -> " +
                        message.getContent()
        );
    }
}

class EmailNotificationStrategy implements NotificationStrategy {

    public void send(User user, Message message) {

        System.out.println(
                "Email Notification to " +
                        user.getName() +
                        " -> " +
                        message.getContent()
        );
    }
}

class NotificationService {

    private final NotificationStrategy strategy;

    public NotificationService(NotificationStrategy strategy) {
        this.strategy = strategy;
    }

    public void notify(User user, Message message) {
        strategy.send(user, message);
    }
}


//class NotificationService{
//   static NotificationService notificationService = new NotificationService();
//    void notifyMembers(List<User> memberList , String message){
//        for(User member : memberList){
//            member.receiveMessage(message);
//        }
//    }
//}

class MessageRepository{
    Map<String , ConcurrentSkipListMap<Long , Message>> messages = new ConcurrentHashMap<>();

    public void saveMessage(String chatId , Message message){
        messages.computeIfAbsent(chatId , x -> new ConcurrentSkipListMap<>()).put(
                message.timestamp.toEpochMilli()
                ,message);
    }
    public List<Message> getMessages(String chatId){
        ConcurrentSkipListMap<Long, Message> map = messages.get(chatId);
        if(map == null) return Collections.emptyList();
        return new ArrayList<>(map.values());
    }

    List<Message> getMessagesBetweenTime(String chatId, Instant from, Instant to){
        ConcurrentSkipListMap<Long , Message> map = messages.get(chatId);
        if(map == null) return Collections.emptyList();

        Map<Long, Message> result = map.subMap(
                from.toEpochMilli(),
                 to.toEpochMilli()
        );
        return new ArrayList<>(result.values());
    }
}

class ChatService{
   Map<String,Chat> chats = new ConcurrentHashMap<>();
   static MessageRepository messageRepository = new MessageRepository();
   static ChatFactory chatFactory = new ChatFactory();
   static NotificationService notificationService = new NotificationService(new PushNotificationStrategy());
    private final ExecutorService executor = Executors.newFixedThreadPool(5);
    public Chat createPrivateChat(User u1, User u2) {

        Chat chat = chatFactory.createPrivateChat(u1, u2);

        chats.put(chat.getId(), chat);

        return chat;
    }

    public Chat createGroupChat(String name, Set<User> users) {

        Chat chat = chatFactory.createGroupChat(name, users);

        chats.put(chat.getId(), chat);

        return chat;
    }

    public void sendMessage(String chatId, User sender, String content) {


        Chat chat = chats.get(chatId);

        if (chat == null) {
            throw new IllegalArgumentException("Chat not found");
        }
        if(!chat.getParticipants().contains(sender)){
            throw new IllegalArgumentException("User not part of chat");
        }

        Message message = new Message(sender, content);


        messageRepository.saveMessage(chatId, message);

        for (User user : chat.getParticipants()) {

            if (!user.getUserId().equals(sender.getUserId())) {

                executor.submit(() -> {
                    notificationService.notify(user, message);
                });
            }
        }
    }

    List<Message> getMessagesBetweenTime(String chatId , Instant start, Instant end) {
       return messageRepository.getMessagesBetweenTime(chatId, start, end);
    }
    List<Message> getMessages(String chatId) {
        return messageRepository.getMessages(chatId);
    }

    public void shutdown(){
        executor.shutdown();
    }


}






public class ChatApplication {

    public static void main(String[] args) {

        ChatService chatService = new ChatService();

        User alice = new User("1", "Alice");
        User bob = new User("2", "Bob");
        User charlie = new User("3", "Charlie");

        Chat privateChat = chatService.createPrivateChat(alice, bob);

        chatService.sendMessage(privateChat.getId(), alice, "Hello Bob!");
        chatService.sendMessage(privateChat.getId(), bob, "Hi Alice!");

        Set<User> groupUsers = new HashSet<>();
        groupUsers.add(alice);
        groupUsers.add(bob);
        groupUsers.add(charlie);

        Chat groupChat = chatService.createGroupChat("Friends", groupUsers);

        chatService.sendMessage(groupChat.getId(), charlie, "Hello everyone!");
        chatService.sendMessage(groupChat.getId(), charlie, "How you doing!");
        List<Message> messages = chatService.getMessages(groupChat.getId());
        System.out.println("Printing Messages");
        for (Message message : messages) {
            System.out.println(message.getContent());
        }

        chatService.shutdown();
    }
}

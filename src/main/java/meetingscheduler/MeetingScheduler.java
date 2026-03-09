package meetingscheduler;
/*
Design a Calendar / Meeting Scheduler system similar to Google Calendar or Microsoft Outlook
 that allows users to schedule meetings with other participants.

The system should support creating, managing, and notifying participants about meetings,
 while ensuring no scheduling conflicts occur for participants.
 User can create Meeting with List of Participants

User can check there own calender
User can other participants availablitiy
Meeting Organizer can cancel the meeting or update the meeting

Core Entities
User , Meeting , Calender
Features -
User -> schedules meeting , modify meeting , cancel meeting

 */

import java.time.LocalDateTime;
import java.util.*;

class User{
    String userId;
    String email;
    Calendar calendar;
    public User(String userId , String email) {
        this.userId = userId;
        this.email = email;
        this.calendar= new Calendar();
    }
}
class Interval {

    LocalDateTime start;
    LocalDateTime end;

    public Interval(LocalDateTime start, LocalDateTime end) {
        this.start = start;
        this.end = end;
    }
}

class MeetingRoom {
    String roomId;
    int capacity;
    List<Interval> bookings = new ArrayList<>();

    public MeetingRoom(String roomId, int capacity) {
        this.roomId = roomId;
        this.capacity = capacity;
    }

    synchronized boolean isAvailable(LocalDateTime start, LocalDateTime end) {
        for (Interval interval : bookings) {
            if (start.isBefore(interval.end) && end.isAfter(interval.start)) {
                return false;
            }
        }
        return true;
    }

    synchronized void book(LocalDateTime start, LocalDateTime end) {
        bookings.add(new Interval(start, end));
    }
}

class Calendar{
    List<Meeting> meetings;

    public Calendar() {
       meetings = new ArrayList<>();
    }
    void addMeeting(Meeting meeting){
        meetings.add(meeting);
    }
    List<Meeting> getMeetingIds() {
        return meetings;
    }

    void removeMeeting(Meeting meeting){
        meetings.remove(meeting);
    }
}

class UserService{
    Map<String , User> users = new HashMap<>();
    static UserService userService = new UserService();
    private UserService(){}

    void createUser(String userId, String email){
        users.put(userId , new User(userId , email));
    }
    User getUser(String userId){
        return users.get(userId);
    }

    boolean isUserAvailable(String userId , LocalDateTime start, LocalDateTime end){
        User user = users.get(userId);
        for(Meeting meeting : user.calendar.meetings){
            if(meeting.meetingStatus== MeetingStatus.CANCELLED) continue;

            if(start.isBefore(meeting.endTime) && end.isAfter(meeting.startTime)){
                return false;
            }
        }
        return true;
    }

}

class Meeting{
    String meetingId;
    String meetingName;
    LocalDateTime startTime;
    LocalDateTime endTime;
    List<User> participants;
    MeetingStatus meetingStatus;
    User organizer;
    String location;

    public Meeting(String meetingId, String meetingName, LocalDateTime startTime, LocalDateTime endTime, List<User> participants, User organizer, String location) {
        this.meetingId = meetingId;
        this.meetingName = meetingName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.participants = participants;
        this.meetingStatus = MeetingStatus.STARTED;
        this.organizer = organizer;
        this.location = location;
    }
}




// ================= NOTIFICATION SERVICE =================

class NotificationService {

    static void notifyUsers(List<User> users, String message) {

        for (User user : users) {
            System.out.println(
                    "Sending notification to "
                            + user.email
                            + " : "
                            + message
            );
        }
    }
}

class RoomService {

    Map<String, MeetingRoom> rooms = new HashMap<>();
    static RoomService roomService = new RoomService();

    private RoomService(){}

    void addRoom(String roomId, int capacity){
        rooms.put(roomId, new MeetingRoom(roomId, capacity));
    }

    MeetingRoom getRoom(String roomId){
        return rooms.get(roomId);
    }

    synchronized boolean bookRoom(String roomId, LocalDateTime start, LocalDateTime end){
        MeetingRoom room = rooms.get(roomId);
        if(room == null) throw new RuntimeException("Room not found");

        if(!room.isAvailable(start, end)){
            return false;
        }

        room.book(start, end);
        return true;
    }
}

class MeetingService{
    Map<String, Meeting> meetings;
    static MeetingService meetingService = new MeetingService();
    UserService userService = UserService.userService;
    RoomService roomService = RoomService.roomService;
    private MeetingService() {
        meetings = new HashMap<>();
    }

    Meeting scheduleMeeting(String meetingId , String meetingName , LocalDateTime
            startTime, LocalDateTime endTime , User organizer , List<User> participants , String location){
        List<User> allUsers = new ArrayList<>(participants);
        allUsers.add(organizer);

        for(User user : allUsers){
            if(!userService.isUserAvailable(user.userId, startTime, endTime)){
                throw new RuntimeException(
                        "User busy: " + user.email
                );
            }
        }

        // Check and book meeting room with concurrency safety
        if(!roomService.bookRoom(location, startTime, endTime)){
            throw new RuntimeException("Room not available: " + location);
        }

        Meeting meeting = new Meeting(meetingId ,meetingName , startTime ,
                endTime , participants , organizer, location);
        meetings.put(meetingId ,meeting);
        for(User user : allUsers){
            user.calendar.addMeeting(meeting);
        }

        // 4. Notify users

        NotificationService.notifyUsers(
                allUsers,
                "Meeting Scheduled: " + meetingName
        );

        return meeting;

    }

    List<Meeting> getUserMeetings(String userId){
        return userService.getUser(userId).calendar.getMeetingIds();
    }


    void cancelMeeting(String meetingId, User requester) {

        Meeting meeting = meetings.get(meetingId);

        if (!meeting.organizer.userId.equals(requester.userId)) {
            throw new RuntimeException(
                    "Only organizer can cancel meeting"
            );
        }

        meeting.meetingStatus = MeetingStatus.CANCELLED;

        List<User> users = new ArrayList<>(meeting.participants);
        users.add(meeting.organizer);

        NotificationService.notifyUsers(
                users,
                "Meeting Cancelled: " + meeting.meetingName
        );
    }public LocalDateTime findEarliestSlot(List<User> users, int durationMinutes) {

    List<Interval> busySlots = new ArrayList<>();


    // 1. Collect all busy slots

    for (User user : users) {

        for (Meeting meeting : user.calendar.getMeetingIds()) {

            if (meeting.meetingStatus == MeetingStatus.CANCELLED)
                continue;

            busySlots.add(
                    new Interval(
                            meeting.startTime,
                            meeting.endTime
                    )
            );
        }
    }


    // 2. Sort by start time

    busySlots.sort(
            Comparator.comparing(a -> a.start)
    );


    // 3. Merge intervals

    List<Interval> merged = new ArrayList<>();

    for (Interval interval : busySlots) {

        if (merged.isEmpty()) {
            merged.add(interval);
        }

        else {

            Interval last = merged.get(merged.size() - 1);

            if (!interval.start.isAfter(last.end)) {

                // overlap

                last.end = max(last.end, interval.end);
            }

            else {
                merged.add(interval);
            }
        }
    }


    // 4. Find gap

    for (int i = 1; i < merged.size(); i++) {

        Interval prev = merged.get(i - 1);
        Interval next = merged.get(i);

        long gap = java.time.Duration
                .between(prev.end, next.start)
                .toMinutes();

        if (gap >= durationMinutes) {

            return prev.end;
        }
    }

    return null;
}

    LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }




    /*
    ============ UPDATE MEETING ============

    Recheck availability
     */

    void updateMeetingTime(String meetingId,
                           LocalDateTime newStart,
                           LocalDateTime newEnd,
                           User requester) {

        Meeting meeting = meetings.get(meetingId);

        if (!meeting.organizer.userId.equals(requester.userId)) {
            throw new RuntimeException(
                    "Only organizer can update"
            );
        }

        List<User> allUsers = new ArrayList<>(meeting.participants);
        allUsers.add(meeting.organizer);


        for (User user : allUsers) {

            if (!userService.isUserAvailable(user.userId, newStart, newEnd)) {
                throw new RuntimeException(
                        "User busy: " + user.email
                );
            }
        }


        meeting.startTime = newStart;
        meeting.endTime = newEnd;

        NotificationService.notifyUsers(
                allUsers,
                "Meeting Updated: " + meeting.meetingName
        );
    }


}

enum MeetingStatus{
    SCHEDULED,STARTED,ENDED,CANCELLED
}

public class MeetingScheduler {




    public static void main(String[] args) {

        UserService userService = UserService.userService;
        MeetingService meetingService = MeetingService.meetingService;

        RoomService roomService = RoomService.roomService;
        roomService.addRoom("Meeting Room A", 10);

        // Create users

        userService.createUser("U1", "alice@company.com");
        userService.createUser("U2", "bob@company.com");
        userService.createUser("U3", "charlie@company.com");


        User alice = userService.getUser("U1");
        User bob = userService.getUser("U2");
        User charlie = userService.getUser("U3");


        List<User> participants = List.of(bob, charlie);


        // Schedule meeting

        meetingService.scheduleMeeting(
                "M1",
                "Design Discussion",
                LocalDateTime.of(2026, 3, 7, 10, 0),
                LocalDateTime.of(2026, 3, 7, 11, 0),
                alice,
                participants,
                "Meeting Room A"
        );


        // Cancel meeting

        meetingService.cancelMeeting("M1", alice);
    }
}

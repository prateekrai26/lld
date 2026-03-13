import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    public static void main(String[] args) {
        RateLimiter rateLimiter= new RateLimiter(2 , 2);

        System.out.println(rateLimiter.allow_request("userID"));
        System.out.println(rateLimiter.allow_request("userID"));
        System.out.println(rateLimiter.allow_request("userID"));
    }
}


class RateLimiter{
    Map<String , ArrayDeque<Long>> userToTimestampMap;
    int maxRequestAllowed;
    int windowSize;
    RateLimiter(Integer maxRequestAllowed , int windowSize){
        this.windowSize = windowSize * 1000;
        userToTimestampMap = new ConcurrentHashMap<>();
        this.maxRequestAllowed = maxRequestAllowed;
    }

    boolean allow_request(String userId){
        if(userToTimestampMap.containsKey(userId)){
            ArrayDeque<Long> timestamp = userToTimestampMap.get(userId);
            long currentTime = System.currentTimeMillis();
            while(!timestamp.isEmpty()){
                Long existingTime = timestamp.peekFirst();
                long diff = currentTime - existingTime;
                if(diff >windowSize) {
                    timestamp.pollFirst();
                }
                else{
                    break;
                }
            }
            if(timestamp.size()< maxRequestAllowed){
                timestamp.offer(currentTime);
                System.out.println("User Request is Accepted");
                return true;
            }
            else {
                System.out.println("User Request is dropped");
                return false;
            }
        }
        else{
            if(maxRequestAllowed > 0){
                long currentTime = System.currentTimeMillis();
                userToTimestampMap.computeIfAbsent(userId , x-> new ArrayDeque<>()).offer(currentTime);
                System.out.println("User Request is Accepted");
                return true;
            }
            else {
                System.out.println("User Request is dropped");
                return false;
            }
        }
    }


}


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiterDemo {

    /* =======================
       1. Strategy Interface
     ======================= */
    interface RateLimitingStrategy {
        boolean allowRequest(String key);
    }

    /* =======================
       2. Context
     ======================= */
    static class RateLimiter {
        private RateLimitingStrategy strategy;

        public RateLimiter(RateLimitingStrategy strategy) {
            this.strategy = strategy;
        }

        public boolean allowRequest(String key) {
            return strategy.allowRequest(key);
        }

        public void setStrategy(RateLimitingStrategy strategy) {
            this.strategy = strategy;
        }
    }

    /* =======================
       3. Fixed Window Strategy
     ======================= */
    static class FixedWindowStrategy implements RateLimitingStrategy {

        private final int maxRequests;
        private final long windowSizeMillis;
        private final Map<String, Window> store = new ConcurrentHashMap<>();

        public FixedWindowStrategy(int maxRequests, long windowSizeMillis) {
            this.maxRequests = maxRequests;
            this.windowSizeMillis = windowSizeMillis;
        }

        @Override
        public boolean allowRequest(String key) {
            long now = System.currentTimeMillis();
            store.putIfAbsent(key, new Window(0, now));

            Window window = store.get(key);

            synchronized (window) {
                if (now - window.startTime >= windowSizeMillis) {
                    window.startTime = now;
                    window.count = 0;
                }

                if (window.count < maxRequests) {
                    window.count++;
                    return true;
                }
                return false;
            }
        }

        static class Window {
            int count;
            long startTime;

            Window(int count, long startTime) {
                this.count = count;
                this.startTime = startTime;
            }
        }
    }

    /* =======================
       4. Sliding Window Log Strategy
     ======================= */
    static class SlidingWindowLogStrategy implements RateLimitingStrategy {

        private final int maxRequests;
        private final long windowSizeMillis;
        private final Map<String, Deque<Long>> store = new ConcurrentHashMap<>();

        public SlidingWindowLogStrategy(int maxRequests, long windowSizeMillis) {
            this.maxRequests = maxRequests;
            this.windowSizeMillis = windowSizeMillis;
        }

        @Override
        public boolean allowRequest(String key) {
            long now = System.currentTimeMillis();
            store.putIfAbsent(key, new LinkedList<>());
            Deque<Long> timestamps = store.get(key);

            synchronized (timestamps) {
                while (!timestamps.isEmpty() &&
                        now - timestamps.peekFirst() > windowSizeMillis) {
                    timestamps.pollFirst();
                }

                if (timestamps.size() < maxRequests) {
                    timestamps.addLast(now);
                    return true;
                }
                return false;
            }
        }
    }

    /* =======================
       5. Token Bucket Strategy
     ======================= */
    static class TokenBucketStrategy implements RateLimitingStrategy {

        private final int capacity;
        private final double refillRatePerSecond;
        private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

        public TokenBucketStrategy(int capacity, double refillRatePerSecond) {
            this.capacity = capacity;
            this.refillRatePerSecond = refillRatePerSecond;
        }

        @Override
        public boolean allowRequest(String key) {
            buckets.putIfAbsent(key, new Bucket(capacity));
            Bucket bucket = buckets.get(key);

            synchronized (bucket) {
                bucket.refill();

                if (bucket.tokens > 0) {
                    bucket.tokens--;
                    return true;
                }
                return false;
            }
        }

        private class Bucket {
            double tokens;
            long lastRefillTime;

            Bucket(int capacity) {
                this.tokens = capacity;
                this.lastRefillTime = System.nanoTime();
            }

            void refill() {
                long now = System.nanoTime();
                double seconds = (now - lastRefillTime) / 1_000_000_000.0;
                double tokensToAdd = seconds * refillRatePerSecond;

                if (tokensToAdd > 0) {
                    tokens = Math.min(capacity, tokens + tokensToAdd);
                    lastRefillTime = now;
                }
            }
        }
    }

    /* =======================
       6. Leaky Bucket Strategy
     ======================= */
    static class LeakyBucketStrategy implements RateLimitingStrategy {

        private final int capacity;
        private final long leakIntervalMillis;
        private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

        public LeakyBucketStrategy(int capacity, long leakIntervalMillis) {
            this.capacity = capacity;
            this.leakIntervalMillis = leakIntervalMillis;
        }

        @Override
        public boolean allowRequest(String key) {
            buckets.putIfAbsent(key, new Bucket());
            Bucket bucket = buckets.get(key);

            synchronized (bucket) {
                bucket.leak();

                if (bucket.currentSize < capacity) {
                    bucket.currentSize++;
                    return true;
                }
                return false;
            }
        }

        private class Bucket {
            int currentSize = 0;
            long lastLeakTime = System.currentTimeMillis();

            void leak() {
                long now = System.currentTimeMillis();
                long leaks = (now - lastLeakTime) / leakIntervalMillis;

                if (leaks > 0) {
                    currentSize = Math.max(0, currentSize - (int) leaks);
                    lastLeakTime = now;
                }
            }
        }
    }

    /* =======================
       7. MAIN METHOD (Testing)
     ======================= */
    public static void main(String[] args) throws InterruptedException {

        String user = "user1";

        System.out.println("===== TOKEN BUCKET =====");
        RateLimiter limiter =
                new RateLimiter(new TokenBucketStrategy(5, 1));

        for (int i = 0; i < 10; i++) {
            System.out.println("Request " + i + ": " + limiter.allowRequest(user));
        }

        Thread.sleep(3000);

        System.out.println("After 3 seconds refill:");
        for (int i = 0; i < 5; i++) {
            System.out.println("Request " + i + ": " + limiter.allowRequest(user));
        }

        System.out.println("\n===== FIXED WINDOW =====");
        limiter.setStrategy(new FixedWindowStrategy(3, 5000));

        for (int i = 0; i < 5; i++) {
            System.out.println("Request " + i + ": " + limiter.allowRequest(user));
        }

        System.out.println("\n===== SLIDING WINDOW =====");
        limiter.setStrategy(new SlidingWindowLogStrategy(3, 5000));

        for (int i = 0; i < 5; i++) {
            System.out.println("Request " + i + ": " + limiter.allowRequest(user));
        }

        System.out.println("\n===== LEAKY BUCKET =====");
        limiter.setStrategy(new LeakyBucketStrategy(3, 1000));

        for (int i = 0; i < 5; i++) {
            System.out.println("Request " + i + ": " + limiter.allowRequest(user));
        }
    }
}
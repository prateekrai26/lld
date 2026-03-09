import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class ConsistentHashingDemo {

    // =======================
    // Consistent Hashing Core
    // =======================
    static class ConsistentHashing {

        private final int numberOfReplicas;
        private final SortedMap<Long, String> circle = new TreeMap<>();

        public ConsistentHashing(int numberOfReplicas) {
            this.numberOfReplicas = numberOfReplicas;
        }

        // Add server with virtual nodes
        public void addServer(String server) {
            for (int i = 0; i < numberOfReplicas; i++) {
                long hash = hash(server + "#" + i);
                circle.put(hash, server);
            }
        }

        // Remove server
        public void removeServer(String server) {
            for (int i = 0; i < numberOfReplicas; i++) {
                long hash = hash(server + "#" + i);
                circle.remove(hash);
            }
        }

        // Get server for key
        public String getServer(String key) {
            if (circle.isEmpty()) {
                return null;
            }

            long hash = hash(key);

            if (!circle.containsKey(hash)) {
                SortedMap<Long, String> tailMap = circle.tailMap(hash);
                hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
            }

            return circle.get(hash);
        }

        // MD5 Hash function
        private long hash(String key) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));

                long hash = 0;
                for (int i = 0; i < 4; i++) {
                    hash <<= 8;
                    hash |= ((int) digest[i]) & 0xFF;
                }
                return hash & 0xffffffffL;

            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // =======================
    // Main Method for Testing
    // =======================
    public static void main(String[] args) {

        ConsistentHashing ch = new ConsistentHashing(3); // 3 virtual nodes per server

        // Add servers
        ch.addServer("Server-1");
        ch.addServer("Server-2");
        ch.addServer("Server-3");

        System.out.println("=== Initial Mapping ===");
        printKeyMappings(ch);

        // Add new server
        System.out.println("\n=== Adding Server-4 ===");
        ch.addServer("Server-4");
        printKeyMappings(ch);

        // Remove a server
        System.out.println("\n=== Removing Server-2 ===");
        ch.removeServer("Server-2");
        printKeyMappings(ch);
    }

    private static void printKeyMappings(ConsistentHashing ch) {
        for (int i = 1; i <= 10; i++) {
            String key = "User-" + i;
            System.out.println(key + " -> " + ch.getServer(key));
        }
    }
}
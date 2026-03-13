package lrucachwithttl;

import java.util.HashMap;
import java.util.Map;

class LRUCacheTTL {

    private final int capacity;
    private final Map<Integer, Node> map = new HashMap<>();

    private Node head;
    private Node tail;

    public LRUCacheTTL(int capacity) {
        this.capacity = capacity;
    }

    public int get(int key) {

        Node node = map.get(key);

        if(node == null)
            return -1;

        if(isExpired(node)) {
            remove(node);
            map.remove(key);
            return -1;
        }

        moveToHead(node);

        return node.value;
    }

    public void put(int key, int value, long ttl) {

        Node node = map.get(key);

        if(node != null) {

            node.value = value;
            node.expiryTime = System.currentTimeMillis() + ttl;

            moveToHead(node);
            return;
        }

        node = new Node(key,value,ttl);

        map.put(key,node);
        addToHead(node);

        if(map.size() > capacity) {

            Node lru = tail;

            remove(lru);
            map.remove(lru.key);
        }
    }

    private boolean isExpired(Node node){
        return System.currentTimeMillis() > node.expiryTime;
    }

    private void moveToHead(Node node){
        remove(node);
        addToHead(node);
    }

    private void addToHead(Node node){

        node.next = head;
        node.prev = null;

        if(head != null)
            head.prev = node;

        head = node;

        if(tail == null)
            tail = node;
    }

    private void remove(Node node){

        if(node.prev != null)
            node.prev.next = node.next;
        else
            head = node.next;

        if(node.next != null)
            node.next.prev = node.prev;
        else
            tail = node.prev;
    }

    static class Node {

        int key;
        int value;
        long expiryTime;

        Node prev;
        Node next;

        Node(int key, int value, long ttl){
            this.key = key;
            this.value = value;
            this.expiryTime = System.currentTimeMillis() + ttl;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        LRUCacheTTL cache = new LRUCacheTTL(2);

        cache.put(1,10,5000);   // ttl 5s
        cache.put(2,20,5000);
        Thread.sleep(5000);
        System.out.println(cache.get(1)); // 10

        cache.put(3,30,5000); // evicts key 2 (LRU)

        System.out.println(cache.get(2)); //-> -1
    }
}
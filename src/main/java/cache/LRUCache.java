package cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class Node<K,V>{
    K key;
    V value;
    Node<K,V> next;
    Node<K,V> prev;
    Node(K key,V value){
        this.key = key;
        this.value = value;
    }
    Node(){
    }
}



public class LRUCache<K,V>{
    Map<K , Node<K,V>> map;
    Node<K,V> head;
    Node<K,V> tail;
    int capacity;

    public LRUCache(int capacity){
        this.capacity = capacity;
        map = new ConcurrentHashMap<>();
        head = new Node<>();
        tail = new Node<>();
        head.next = tail;
        tail.prev = head;
    }

    synchronized void put(K key, V value){
        Node<K,V> node = map.get(key);
        if(node != null){
            node.value = value;
            moveToHead(node);
        }
        else{
            node = new Node<>(key,value);
            map.put(key, node);
            addToHead(node);
            if(map.size() > capacity){
                Node<K,V> tailNode = removeTail();
                map.remove(tailNode.key);
            }
        }
    }

    synchronized V get(K key){
        Node<K,V> node = map.get(key);
        if(node == null){
            return null;
        }
        moveToHead(node);
        return node.value;
    }

    synchronized void addToHead(Node<K,V> node){
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }

    synchronized void deleteNode(Node<K,V> node){
        node.next.prev = node.prev;
        node.prev.next = node.next;
    }

   synchronized void moveToHead(Node<K,V> node){
        deleteNode(node);
        addToHead(node);
    }

   synchronized Node<K,V> removeTail(){
       Node<K,V> tailNode = tail.prev;
       deleteNode(tailNode);
       return tailNode;
    }
    public static void main(String[] args) {
            LRUCache<Integer,Integer> lruCache = new LRUCache<>(2);
            lruCache.put(1, 2);
            lruCache.put(2, 3);
            lruCache.put(3, 4);
            System.out.println(lruCache.get(1));
    }
}

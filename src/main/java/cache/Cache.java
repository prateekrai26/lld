package cache;

/*
  Implement LRU Cache with TTL expiry

 */

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
//
//class CacheItem{
//    String key;
//    String value;
//    Instant expiryTime;
//    public CacheItem(String key, String value, Instant expiryTime){
//        this.key = key;
//        this.value = value;
//        this.expiryTime = expiryTime;
//    }
//}

class ConcurrentLRUCache<K,V>{
    int capacity;
    Map<K,V> cache;
    final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    ConcurrentLRUCache(int capacity){
        this.capacity = capacity;
       cache = new LinkedHashMap<K,V>(capacity , 0.75f , true){
           boolean removeEldestEntry(Map<K,V> eldest){
               return size() > capacity;
           }
       };
    }

    public V get(K key){
        lock.writeLock().lock();
        V value = null;
        try{
           value = cache.get(key);
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
        finally{
            lock.writeLock().unlock();
        }
        return value;
    }
    public void put(K key, V value){
        lock.writeLock().lock();
        try {
            cache.put(key, value);
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
        finally {
            lock.writeLock().unlock();
        }
    }
}
public class Cache {

    public static void main(String[] args) {
        ConcurrentLRUCache<Integer , Integer> cache = new ConcurrentLRUCache<>(5);
        cache.put(1, 1);
        System.out.println(cache.get(1));
    }
}

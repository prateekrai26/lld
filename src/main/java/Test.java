import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

class MyThread implements Runnable {

    @Override
    public void run() {
        System.out.println("Running thread");
    }
}
public class Test {

    public static void main(String[] args) {
        PriorityQueue<Integer> pq = new PriorityQueue<>();
        pq.add(5);
        pq.add(3);
        pq.add(41);
        System.out.println(pq.poll());
        Map<Integer , Integer> map = Map.of(1,2,3,4,5,6);

        map.forEach((x,y) -> System.out.println(x + " " + y));
        List<Integer> ls = new ArrayList<>(map.values());
        System.out.println(ls);
        System.out.println(map.values());

        PriorityQueue<Integer> pqq = new PriorityQueue<>(map.values());
        System.out.println(pqq);

        System.out.println(pqq.poll());
        System.out.println(pqq.poll());
        ArrayList<Integer> list = new ArrayList<>(map.values());
        System.out.println(list);

        MyThread myThread = new MyThread();
        new Thread(myThread).start();

    }
}

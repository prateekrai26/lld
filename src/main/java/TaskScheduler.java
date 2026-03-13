import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

class Task {
   final String taskId = UUID.randomUUID().toString();
    final Runnable job;
    final long delay;
    final int maxRetries ;

    String workerId;
    TaskState taskState;
    int retries = 0;
    Task(Runnable job, long delay, int maxRetries){
        this.job = job;
        this.delay = System.currentTimeMillis() + delay;
        this.maxRetries = maxRetries;
    }
}

enum TaskState{
    SCHEDULED,RUNNING,COMPLETED,FAILED
}


class TaskStore{
    Map<String , Task> taskStore = new ConcurrentHashMap<>();
    void addTask(Task task){
        taskStore.put(task.taskId , task);
    }

    Task getTask(String taskId){
        return taskStore.get(taskId);
    }

}

class Scheduler{
    PriorityQueue<Task> queue = new PriorityQueue<>(Comparator.comparingLong((x) -> x.delay));

    synchronized void submitTask(Task task){
        queue.add(task);
        notifyAll();
    }

    synchronized Task pollTask(String workerId){

        while(true) {

            while (queue.isEmpty()) {
                try {
                    System.out.println(workerId + " is Waiting  for task");
                    wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            long now = System.currentTimeMillis();

            Task task = queue.peek();
            long waitTime  = task.delay - now;
            if (waitTime > 0) {
                try {
                    wait(waitTime);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                continue;
            }
            else {
                task = queue.poll();
            }
            task.workerId = workerId;
            return task;
        }
    }

    synchronized void rescheduleTask(Task task){
        task.taskState = TaskState.SCHEDULED;
        task.workerId = null;
        queue.offer(task);
        notifyAll();
    }

}

class Worker implements Runnable{
    String workerId;
    Scheduler scheduler;
    Worker(String workerId , Scheduler scheduler){
        this.scheduler = scheduler;
        this.workerId = workerId;
    }
    @Override
    public void run() {
            while(true){
                Task task = scheduler.pollTask(this.workerId);
                try{
                    task.taskState = TaskState.RUNNING;
                    task.job.run();
                    System.out.println("Task Completed for " + task.taskId  + "by " + workerId);
                    task.taskState = TaskState.COMPLETED;
                }
                catch (Exception e){
                    if(task.retries < task.maxRetries){
                        task.retries++;
                        scheduler.rescheduleTask(task);
                    }
                    else{
                        task.taskState = TaskState.FAILED;
                    }
                }
            }
    }

    private void sleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            System.out.println("sleep interrupted");
        }
    }
}

public class TaskScheduler {

    public static void main(String[] args) throws InterruptedException {
        Scheduler scheduler = new Scheduler();
        new Thread(new Worker("Worker-1" , scheduler)).start();
        new Thread(new Worker("Worker-2" , scheduler)).start();
        scheduler.submitTask(new Task(()-> {
            System.out.println("Running Task1");

            } ,10000 , 2));
        Thread.sleep(5000);
        scheduler.submitTask(new Task(()-> System.out.println("Running Task2") ,0 , 2));
    }
}

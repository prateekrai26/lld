import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

class Task{
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
        long now = System.currentTimeMillis();
        if(queue.isEmpty()){ return null;}
        Task task = queue.peek();
        if(task.delay > now) return  null;
        task.workerId = workerId;
        return task;
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
                if(task == null){
                    sleep(300);
                    continue;
                }
                try{
                    task.taskState = TaskState.RUNNING;
                    task.job.run();
                    System.out.println("Task Completed for " + task.taskId );
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

    public static void main(String[] args) {
        Scheduler scheduler = new Scheduler();
        new Thread(new Worker("Worker-1" , scheduler)).start();
        new Thread(new Worker("Worker-2" , scheduler)).start();
        scheduler.submitTask(new Task(()-> {
            System.out.println("Running Task1");

            } ,2000 , 2));
        scheduler.submitTask(new Task(()-> System.out.println("Running Task2") ,2000 , 2));
    }
}

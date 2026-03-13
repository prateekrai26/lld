package taskscheduler;

import java.util.UUID;
import java.util.concurrent.*;

class Task implements Delayed {

    final String taskId = UUID.randomUUID().toString();
    final Runnable job;
    final int maxRetries;
    int retries = 0;

    private final long scheduledTime;

    Task(Runnable job, long delayMillis, int maxRetries) {
        this.job = job;
        this.maxRetries = maxRetries;
        this.scheduledTime = System.currentTimeMillis() + delayMillis;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long diff = scheduledTime - System.currentTimeMillis();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        Task o = (Task) other;
        return Long.compare(this.scheduledTime, o.scheduledTime);
    }
}

class Scheduler {

    private final DelayQueue<Task> queue = new DelayQueue<>();
    private final ExecutorService workers;

    Scheduler(int workerCount) {
        workers = Executors.newFixedThreadPool(workerCount);

        for(int i=0;i<workerCount;i++) {
            workers.submit(this::workerLoop);
        }
    }

    void submitTask(Task task) {
        queue.put(task);
    }

    private void workerLoop() {
        while(true) {
            try {

                Task task = queue.take(); // blocks until delay expires

                try {
                    task.job.run();
                    System.out.println("Task completed: " + task.taskId);
                }
                catch(Exception e) {

                    if(task.retries < task.maxRetries) {
                        task.retries++;
                        System.out.println("Retrying task " + task.taskId);
                        queue.put(task);
                    }
                    else {
                        System.out.println("Task failed: " + task.taskId);
                    }

                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

public class TaskScheduler {

    public static void main(String[] args) {

        Scheduler scheduler = new Scheduler(2);

        scheduler.submitTask(new Task(() ->
                System.out.println("Running Task1"), 10000, 2));

        scheduler.submitTask(new Task(() ->
                System.out.println("Running Task2"), 5000, 2));
    }
}
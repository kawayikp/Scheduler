package scheduling;

/**
 * Created by Juntong Liu.
 */

import java.util.Scanner;
import java.util.List;
import java.util.LinkedList;

class RandomProcess extends Process{
    static final int MAX_SLICE = 4;
    public RandomProcess(String name, int create_time, int max_burst_second, int max_priority) {
        super(name, create_time, (int)(Math.random() * max_priority), new LinkedList<>());
        int num_compute_slice = (int)(Math.random() * MAX_SLICE) + 1;
        for (int i = 0; i < num_compute_slice - 1; ++i) {
            this.timeSlices.add(new TimeSlice((int)(Math.random() * max_burst_second) + 1, OperationType.COMPUTE));
            this.timeSlices.add(new TimeSlice((int)(Math.random() * max_burst_second) + 1, OperationType.IO));
        }
        this.timeSlices.add(new TimeSlice((int)(Math.random() * max_burst_second) + 1, OperationType.COMPUTE));
    }

    public RandomProcess(Process p) {
        super(p.name, p.create_time, p.priority, new LinkedList<>());
        for (TimeSlice ts : p.timeSlices) {
            this.timeSlices.add(new TimeSlice(ts.time, ts.type));
        }
        this.finished = p.finished;
        this.finish_time = p.finish_time;
    }
}

public class SchedulingRunner {
    static final int MAX_TIME_SLOT = 5;
    static final int BURST_SECOND = 5;
    private static boolean allSchedulerFinished(List<Scheduler> schedulers) {
        for (Scheduler scheduler : schedulers) {
            if (!scheduler.finished()) {
                return false;
            }
        }
        return true;
    }
    public static void main(String[] args) {
        List<Process> processes = new LinkedList<>();
        List<Scheduler> schedulers = new LinkedList<>();
        // Add all schedulers here:
        schedulers.add(new O1Scheduler(BURST_SECOND));
        schedulers.add(new PriorityScheduler());
        schedulers.add(new RoundRobinScheduler(BURST_SECOND));
        schedulers.add(new CFScheduler(BURST_SECOND));
        Scanner scan = new Scanner(System.in);
        System.out.println("Please input how many process you want to create: ");
        int process_num = scan.nextInt();
        int time_counter = 0;
        for (int i = 0; i < process_num; ++i) {
            processes.add(new RandomProcess(Integer.toString(i), time_counter, BURST_SECOND * 4, 140));
            time_counter += (int) (Math.random() * MAX_TIME_SLOT);
        }
        int counter = 0;
        while (!allSchedulerFinished(schedulers) || !processes.isEmpty()) {
            while (!processes.isEmpty() && counter == processes.get(0).create_time) {
                for (Scheduler scheduler : schedulers) {
                    scheduler.enqueueProcess(new RandomProcess(processes.get(0)));
                }
                System.out.println("Add process : " + processes.get(0).toString());
                processes.remove(0);
            }
            for (Scheduler scheduler : schedulers) {
                scheduler.run();
                scheduler.IO();
            }

            counter += 1;
        }
        System.out.println("Name\t\t\tThroughput\tTurnaround time\t\tWaiting time\t\t" +
                "Response time\tWeighted Turnaround time");
        for (Scheduler scheduler : schedulers) {
            List<String> summary = scheduler.summary();
            for (String s : summary) {
                System.out.print(s + "\t");
            }
            System.out.println("");
        }
    }
}

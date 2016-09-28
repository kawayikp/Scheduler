package scheduling;

/**
 * Created by Yue Liu.
 */

import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

class PriorityArray {
    static final int MAX_PRIORITY = 140;
    boolean map[] = null;
    ArrayList<List<Process>>process_array;					
    int find_priority() {
        for (int i = 0; i < MAX_PRIORITY; ++i) {
            if (map[i] == true) {
                return i;
            }
        }
        return -1;
    }
    Process getProcess(int priority) {
        List<Process> list = process_array.get(priority);
        return list.get(0);
    }
    boolean removeProcess(Process p) {
        boolean result = process_array.get(p.priority).remove(p);
        if(process_array.get(p.priority).isEmpty()) {
            map[p.priority] = false;
        }
        return result;
    }
    void addProcess(Process p) {
        process_array.get(p.priority).add(p);
        map[p.priority] = true;
    }
    PriorityArray() {
        map = new boolean[MAX_PRIORITY];
        process_array = new ArrayList<>();
        for (int i = 0; i < MAX_PRIORITY; ++i) {
            map[i] = false;
            process_array.add(new LinkedList<>());
        }
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < process_array.size(); ++i) {
            if (process_array.get(i).isEmpty() == map[i]) {
                System.out.println("index "+ i + " size " + process_array.get(i).isEmpty() + "Something goes wrong wildly! bitmap disagree with array!");
            }
            if (!process_array.get(i).isEmpty()) {
                sb.append("For priority ").append(i).append(" there are ").append(process_array.get(i).size()).
                        append(" process.\n");
                for (Process p : process_array.get(i)) {
                    sb.append(p.toString()).append("\n");
                }
            }
        }
        return sb.toString();
    }
}

public class PriorityScheduler implements Scheduler {
    private PriorityArray prio_array = null;
    private boolean finished;
    private int counter = 0;
    private Process runningProcess;
    private List<Process> ioProcessList;
    private List<Process> finishedProcessList;
    PriorityScheduler() {
        this.prio_array = new PriorityArray();
        this.finished = false;
        this.counter = 0;
        this.runningProcess = null;
        this.ioProcessList = new LinkedList<>();
        this.finishedProcessList = new LinkedList<>();
    }
    protected void switchNextProcess() {			
        if (this.runningProcess != null) {
            if (this.runningProcess.isFinished()) {
            	prio_array.removeProcess(this.runningProcess);
                this.runningProcess.setFinishTime(counter);
                finishedProcessList.add(this.runningProcess);
            } else {
            	this.prio_array.removeProcess(this.runningProcess);
                if (this.runningProcess.requestIO()) {
                    ioProcessList.add(this.runningProcess);
                } else {
                	System.out.println("something wrong!");
                }
            }
        }
        int priority = this.prio_array.find_priority();
        if (priority < 0) {
            if (ioProcessList.isEmpty()) {
                finished = true;
            }
            this.runningProcess = null;
            return;
        }
        this.runningProcess = this.prio_array.getProcess(priority);
        this.runningProcess.setResponseTime(counter);			
    }
    private void processWait() {
        for (List<Process> process_list : prio_array.process_array) {
            for (Process p : process_list) {
                if (p != this.runningProcess) {
                    if (!p.processWait()) {
                        System.err.println("ActiveArray has process finised or doing IO! Process: " + p.toString());
                    }
                }
            }
        }

    }
    public void run() {
        if (this.finished) {
            return;
        }
        // Process run 1 time slot.
        if (runningProcess == null ||						
                this.runningProcess.requestIO() ||
                runningProcess.isFinished()) {				
            switchNextProcess();							
        }													
        counter += 1;	
        // No process to run.
        if (runningProcess == null) {						
            return;
        }
        processWait();
        if (this.runningProcess.exec(OperationType.COMPUTE)) {
            if (this.runningProcess.requestIO()) {
                switchNextProcess();
            }
        } else {
            System.out.println("ERROR happens it should never reached here");
        }
    }
    public void IO() {
        if (ioProcessList.isEmpty()) {
            return;
        }
        Process p = ioProcessList.get(0);
        if (p.exec(OperationType.IO)) {
            if (!p.requestIO()) {
                ioProcessList.remove(p);
                prio_array.addProcess(p);
            }
        }
        else {
            System.out.println("ERROR happens process in IO list should be able to do IO");
        }
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("The IO array: \n");
        for (Process p : ioProcessList) {
            sb.append(p.toString()).append("\n");
        }
        sb.append("\n").append("The finished array:\n");
        for (Process p : finishedProcessList) {
            sb.append(p.toString()).append("\n");
        }
        return sb.toString();
    }
    public void enqueueProcess(Process p) {
    	prio_array.addProcess(p);
        this.finished = false;
    }
    public boolean finished(){
        return this.finished;
    }
    public List<String> summary() {
        if (!finished() || this.finishedProcessList.isEmpty()) {
            return null;
        }
        double total_turnaround_time = 0.0;
        double total_weighted_turnaround_time = 0.0;
        double total_wait_time = 0;
        double total_response_time = 0;
        for (Process p : this.finishedProcessList) {
            double turnaround = (double)(p.finish_time - p.create_time);
            total_turnaround_time += turnaround;
            total_weighted_turnaround_time += turnaround * (PriorityArray.MAX_PRIORITY - p.priority);
            total_wait_time += p.waiting_time;
            total_response_time += p.response_time;

        }
        ArrayList<String> result = new ArrayList<>();
        result.add(getName());
        result.add(new DecimalFormat("##.##").format((double)this.finishedProcessList.size() / (counter - 1)) + "\t");
        result.add(new DecimalFormat("##.##").format(total_turnaround_time / finishedProcessList.size())+ "\t");
        result.add(new DecimalFormat("##.##").format(total_wait_time / finishedProcessList.size())+ "\t");
        result.add(new DecimalFormat("##.##").format(total_response_time / finishedProcessList.size())+ "\t");
        result.add(new DecimalFormat("##.##").format(total_weighted_turnaround_time)+ "\t");
        return result;
    }

    public String getName() {
        return "PriorityScheduler      ";
    }
}


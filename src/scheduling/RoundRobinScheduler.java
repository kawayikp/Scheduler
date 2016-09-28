package scheduling;

/**
 * Created by Yue Liu.
 */

import java.util.Scanner;
import java.util.List;
import java.util.LinkedList;
import java.text.DecimalFormat;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

class RRArray {
    static final int MAX_PRIORITY = 140;
	List<Process> process_array;
    boolean find_nextprocess() {                       
    	if (process_array.size() == 0) {
    		return false;
    	}
    	return true;
    }
    Process getProcess() {
        return process_array.get(0);
    }
    boolean removeProcess(Process p) {
        boolean result = process_array.remove(p);
        return result;
    }
    void addProcess(Process p) {
        process_array.add(p);
    }
    RRArray() {
        process_array = new LinkedList<>();
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < process_array.size(); ++i) {
            if (!process_array.isEmpty()) {
                sb.append("For priority ").append(i).append(" there are ").append(process_array.size()).
                        append(" process.\n");
                for (Process p : process_array) {
                    sb.append(p.toString()).append("\n");
                }
            }
        }
        return sb.toString();
    }
}

public class RoundRobinScheduler implements Scheduler {
    private RRArray rr_array = null;
    private boolean finished;
    private int counter = 0;
    private int burstSecond = 0;
    private int remainingSecond = 0;
    private Process runningProcess;
    private List<Process> ioProcessList;
    private List<Process> finishedProcessList;
    RoundRobinScheduler(int burstSecond) {
        this.rr_array = new RRArray();
        this.finished = false;
        this.counter = 0;
        this.burstSecond = burstSecond;
        this.runningProcess = null;
        this.ioProcessList = new LinkedList<>();
        this.finishedProcessList = new LinkedList<>();
    }
    protected void switchNextProcess() {
        if (this.runningProcess != null) {
            if (this.runningProcess.isFinished()) {
            	rr_array.removeProcess(this.runningProcess);
                this.runningProcess.setFinishTime(counter);
                finishedProcessList.add(this.runningProcess);
            } else {
            	rr_array.removeProcess(this.runningProcess);
                if (this.runningProcess.requestIO()) {
                    ioProcessList.add(this.runningProcess);
                } else {
                	rr_array.addProcess(this.runningProcess);
                }
            }
        }
        if (!rr_array.find_nextprocess()) {
            if (ioProcessList.isEmpty()) {
                finished = true;
            }
            this.runningProcess = null;
            return;
            }
        this.runningProcess = rr_array.getProcess();
        this.runningProcess.setResponseTime(counter);
    }
    private void processWait() {
        for (Process p : rr_array.process_array) {
            if (p != this.runningProcess) {
                if (!p.processWait()) {
                    System.err.println("ActiveArray has process finised or doing IO! Process: " + p.toString());
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
                this.remainingSecond == 0 ||
                this.runningProcess.requestIO() ||
                runningProcess.isFinished()) {
            switchNextProcess();
            this.remainingSecond = this.burstSecond;
        }
        counter += 1;
        // No process to run.
        if (runningProcess == null) {
            return;
        }
        processWait();
        if (this.runningProcess.exec(OperationType.COMPUTE)) {
            this.remainingSecond -= 1;
            if (this.runningProcess.requestIO()) {
                switchNextProcess();
                this.remainingSecond = this.burstSecond;
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
                rr_array.addProcess(p);
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
    	rr_array.addProcess(p);
        //this.finished = false;
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
            total_weighted_turnaround_time += turnaround * (RRArray.MAX_PRIORITY - p.priority);
            total_wait_time += p.waiting_time;
            total_response_time += p.response_time;

        }
        ArrayList<String> result = new ArrayList<>();
        result.add(getName());
        result.add(new DecimalFormat("##.##").format((double)this.finishedProcessList.size() / (counter - 1)) + "\t");
        result.add(new DecimalFormat("##.##").format(total_turnaround_time / finishedProcessList.size()) + "\t");
        result.add(new DecimalFormat("##.##").format(total_wait_time / finishedProcessList.size()) + "\t");
        result.add(new DecimalFormat("##.##").format(total_response_time / finishedProcessList.size()) + "\t");
        result.add(new DecimalFormat("##.##").format(total_weighted_turnaround_time));
        return result;
    }

    public String getName() {
        return "RoundRobinScheduler";
    }
}
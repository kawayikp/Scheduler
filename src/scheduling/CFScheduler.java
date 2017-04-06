package scheduling;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CFScheduler implements Scheduler{
    private AvlTree<CFSProcess> avl;
    private boolean finished;
    private int counter;
    private int burstSecond;
    private int remainingSecond;
    private int idealTime;
    private CFSProcess runningProcess;
    private int totalWeight;
    private List<CFSProcess> ioProcessList;
    private List<CFSProcess> finishedProcessList;
    private int IOcounter = 0;
    private int CPUCounter = 0;
    private String IOProcessName = null;
    private List<CFSProcess> activeCFSProcess;
    private int weight;
    private int nice_value;


    public static int weight0 = 1024;
    public static int nice_value_high = 19;
    public static int nice_value_low = -20;
    public static int priority_high = 0;
    public static int priority_low = 139;
    public static int targeted_latency = 5;

    public CFScheduler(int burstSecond) {
        this.avl = new AvlTree<CFSProcess>();
        this.burstSecond = burstSecond;
        this.totalWeight = 0;
        this.runningProcess = null;
        this.finished = false;
        this.idealTime = 0;
        this.ioProcessList = new LinkedList<>();
        this.finishedProcessList = new LinkedList<>();
        this.activeCFSProcess = new LinkedList<>();
        this.weight = 0;


    }


    public void enqueueProcess(Process p) {
        CFSProcess cfsProcess = new CFSProcess(p.name, p.create_time, p.priority, p.timeSlices, 0);
        cfsProcess.vruntime = CFScheduler.targeted_latency * (CFScheduler.weight0 * 1.0 / (this.totalWeight + cfsProcess.weight));
        avl.insert(cfsProcess);
        this.activeCFSProcess.add(cfsProcess);
        this.totalWeight += cfsProcess.weight;

    }


    public int setUpIdealTime() {

        this.idealTime = Math.max(CFScheduler.targeted_latency * (this.runningProcess.weight  / this.totalWeight), 1);
        this.idealTime = Math.min(idealTime, CFScheduler.targeted_latency);
        return this.idealTime;
    }

    public double updateVruntime() {
        double delta = this.idealTime * (CFScheduler.weight0 / this.runningProcess.weight);
        return this.runningProcess.vruntime + delta;
    }

    protected void switchNextProcess() {
        if (this.runningProcess != null) {
            if (this.runningProcess.isFinished()) {
                avl.delete(this.runningProcess);
                this.activeCFSProcess.remove(this.runningProcess);
                this.totalWeight -= this.runningProcess.weight;
                this.runningProcess.setFinishTime(counter);
                finishedProcessList.add(this.runningProcess);
            } else {
                if (this.runningProcess.requestIO()) {
                    avl.delete(this.runningProcess);
                    this.activeCFSProcess.remove(this.runningProcess);
                    this.runningProcess.setUpVruntime(updateVruntime());
                    this.totalWeight -= this.runningProcess.weight;
                    this.IOProcessName = this.runningProcess.name;


                    ioProcessList.add(this.runningProcess);

                } else {
                    avl.delete(runningProcess);
                    this.activeCFSProcess.remove(this.runningProcess);
                    this.runningProcess.setUpVruntime(updateVruntime());
                    this.totalWeight -= this.runningProcess.weight;

                    avl.insert(runningProcess);
                    this.activeCFSProcess.add(runningProcess);
                    this.totalWeight += this.runningProcess.weight;
                }
            }
        }

        if(avl.isEmpty() ) {
            if (this.ioProcessList.isEmpty()) {
                finished = true;
            }
            this.runningProcess = null;
            return;
        }
        this.runningProcess = avl.getSmallest().getElement();
        this.runningProcess.setResponseTime(counter);

    }

    private void processWait() {
        if (this.activeCFSProcess.isEmpty()) return;

        if (this.activeCFSProcess.size() != avl.size()) {
            System.err.println("active processes list cannot match with avl tree");
        }

        for (CFSProcess item: this.activeCFSProcess) {
            if (!item.name.equals(this.runningProcess.name)) {
                boolean temp = item.processWait();
                if (!temp) {
                    System.err.println("AVL tree has process finised or doing IO! Process: " + item.toString());
                }
            }
        }

    }

    public void run() {
        if (runningProcess == null ||
                this.remainingSecond == 0 ||
                this.runningProcess.requestIO() ||
                runningProcess.isFinished()) {
            switchNextProcess();
            if(this.runningProcess != null) {
                this.remainingSecond = this.setUpIdealTime();
            }
        }
        // No process to run.
        counter += 1;
        processWait();
        if (runningProcess == null) {
            return;
        }
        if (this.runningProcess.exec(OperationType.COMPUTE)) {
            CPUCounter++;
            this.remainingSecond -= 1;
            if (this.runningProcess.requestIO()) {
                switchNextProcess();
                if(this.runningProcess != null) {
                    this.remainingSecond = this.setUpIdealTime();
                }
            }
        } else {
            System.out.println("ERROR happens it should never reached here");
        }
    }


    public boolean finished() {
        return this.finished;
    }


    public void IO() {

        if (ioProcessList.isEmpty()) {
            return;
        }

        CFSProcess p = ioProcessList.get(0);
        if (p.exec(OperationType.IO)) {
            if (!p.requestIO()) {
                avl.insert(p);
                this.activeCFSProcess.add(p);
                this.totalWeight += p.weight;
                ioProcessList.remove(p);

            }
        }
        else {
            System.out.println("ERROR happens process in IO list should be able to do IO");
        }


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
            total_weighted_turnaround_time += turnaround * (PrioArray.MAX_PRIORITY - p.priority);
            total_wait_time += p.waiting_time;
            total_response_time += p.response_time;

        }
        ArrayList<String> result = new ArrayList<>();
        result.add(getName());
        result.add(new DecimalFormat("##.##").format((double)this.finishedProcessList.size() / (counter -1)) + "\t");
        result.add(new DecimalFormat("##.##").format(total_turnaround_time / finishedProcessList.size())+ "\t");
        result.add(new DecimalFormat("##.##").format(total_wait_time / finishedProcessList.size())+ "\t");
        result.add(new DecimalFormat("##.##").format(total_response_time / finishedProcessList.size())+ "\t");
        result.add(new DecimalFormat("##.##").format(total_weighted_turnaround_time)+ "\t");
        return result;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("The avl tree is empty: \n").append(avl.isEmpty()).append("\n");
        sb.append("The IO array: \n");
        for (CFSProcess p : ioProcessList) {
            sb.append(p.toString()).append("\n");
        }
        sb.append("\n").append("The finished array:\n");
        for (CFSProcess p : finishedProcessList) {
            sb.append(p.toString()).append("\n");
        }
        return sb.toString();
    }

    public String getName() {
        return "CFScheduler           ";
    }


}



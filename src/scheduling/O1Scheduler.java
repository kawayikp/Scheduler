package scheduling;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

class PrioArray {
    static final int MAX_PRIORITY = 140;
    boolean map[] = null;
    ArrayList<List<O1Process>>process_array;
    int find_priority() {
        for (int i = 0; i < MAX_PRIORITY; ++i) {
            if (map[i] == true) {
                return i;
            }
        }
        return -1;
    }
    O1Process getProcess(int priority) {
        List<O1Process> list = process_array.get(priority);
        return list.get(0);
    }
    boolean removeProcess(O1Process p) {
        boolean result = process_array.get(p.getDynamicPriority()).remove(p);
        if(process_array.get(p.getDynamicPriority()).isEmpty()) {
            map[p.getDynamicPriority()] = false;
        }
        return result;
    }
    void addProcess(O1Process p) {
        process_array.get(p.getDynamicPriority()).add(p);
        map[p.getDynamicPriority()] = true;
    }
    PrioArray(boolean isActive) {
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

class O1Process extends Process {
    private int sleep_time;
    private int dynamic_priority;
    private int io_start;
    private int run_time;
    private int time_slice;
    private final static int PRIORITY_BOOST = 10;
    private final static int MAX_SLEEP_TIME = 100;
    private final static int MIN_USER_PRIORITY = 100;
    private final static int MAX_TIME_SLICE = 45;
    private final static int MAX_PRIORITY = 139;
    private final static int MIN_TIME_SLICE = 5;
    O1Process(Process p) {
        super(p.name, p.create_time, p.priority, new LinkedList<>());
        for (TimeSlice ts : p.timeSlices) {
            this.timeSlices.add(new TimeSlice(ts.time, ts.type));
        }
        this.finished = p.finished;
        this.finish_time = p.finish_time;
        this.sleep_time = 0;
        this.dynamic_priority = p.priority;
        this.io_start = -1;
        this.run_time = 0;
        if (this.priority < MIN_USER_PRIORITY) {
            this.time_slice = (MAX_TIME_SLICE + MIN_TIME_SLICE) / 2;
        } else {
            this.time_slice = MIN_TIME_SLICE +
                    (MAX_PRIORITY - this.priority) * (MAX_TIME_SLICE - MIN_TIME_SLICE) / MAX_TIME_SLICE;
        }
    }

    @Override
    public boolean exec(OperationType type) {
        boolean status = super.exec(type);
        if (type == OperationType.COMPUTE && status == true) {
            this.run_time++;
        }
        return status;
    }

    public boolean needSwitch(){
        if(this.run_time < this.time_slice) {
            return false;
        }
        return true;
    }

    void prioCalu() {
        this.run_time = 0;
        if (this.priority < MIN_USER_PRIORITY) {
            return;
        }
        if (this.sleep_time > MAX_SLEEP_TIME) {
            this.sleep_time = MAX_SLEEP_TIME;
        }
        if (this.sleep_time < 0) {
            this.sleep_time = 0;
        }
        this.dynamic_priority = this.priority - (
                (int)((this.sleep_time / MAX_SLEEP_TIME) * PRIORITY_BOOST + 0.5) - PRIORITY_BOOST / 2);
        if (this.dynamic_priority > 139) {
            this.dynamic_priority = 139;
        }
        if (this.dynamic_priority < 0) {
            this.dynamic_priority = 0;
        }
    }
    void prioCaluAfterCompute() {
        this.sleep_time -= this.run_time;
        prioCalu();
    }
    void prioCaluBeforeIO(int time_stamp) {
        this.io_start = time_stamp;
    }
    void prioCaluAfterIO(int time_stamp) {
        if (this.io_start < 0) {
            System.err.println("Some thing is wrong, PrioCaluAfterIO call with minus 0 io start time.");
        }
        this.sleep_time += time_stamp - this.io_start;
        this.io_start = -1;
        prioCalu();
    }
    int getDynamicPriority() {
        if (this.priority < MIN_USER_PRIORITY) {
            return this.priority;
        }
        return this.dynamic_priority;
    }
}

public class O1Scheduler implements Scheduler {
    private PrioArray prio_array[] = null;
    private int active;
    private boolean finished;
    private int counter = 0;
    private int burstSecond = 0;
    private int remainingSecond = 0;
    private O1Process runningProcess;
    private List<O1Process> ioProcessList;
    private List<O1Process> finishedProcessList;
    O1Scheduler(int burstSecond) {
        this.prio_array = new PrioArray[2];
        this.prio_array[0] = new PrioArray(true);
        this.prio_array[1] = new PrioArray(false);
        this.active = 0;
        this.finished = false;
        this.counter = 0;
        this.burstSecond = burstSecond;
        this.runningProcess = null;
        this.ioProcessList = new LinkedList<>();
        this.finishedProcessList = new LinkedList<>();
    }
    private PrioArray getActiveArray(){
        return this.prio_array[this.active];
    }
    private PrioArray getExpireArray(){
        return this.prio_array[(this.active + 1) % 2];
    }
    protected void switchNextProcess() {
        if (this.runningProcess != null) {
            // process finished.
            if (this.runningProcess.isFinished()) {
                getActiveArray().removeProcess(this.runningProcess);
                this.runningProcess.setFinishTime(counter);
                finishedProcessList.add(this.runningProcess);
            }
            // process request io.
            else if (this.runningProcess.requestIO()) {
                    this.runningProcess.prioCaluBeforeIO(counter);
                    ioProcessList.add(this.runningProcess);
                    getActiveArray().removeProcess(this.runningProcess);
            }
            // Run out of time slice.
            else if (this.runningProcess.needSwitch()) {
                getActiveArray().removeProcess(this.runningProcess);
                this.runningProcess.prioCaluAfterCompute();
                getExpireArray().addProcess(this.runningProcess);
            }
            // Get preempted. don't touch it.
        }
        int priority = getActiveArray().find_priority();
        if (priority < 0) {
            // Swapping the active array;
            this.active = (this.active + 1) % 2;
            // both array is empty, no more process to process.
            priority = getActiveArray().find_priority();
            if (priority < 0) {
                if (ioProcessList.isEmpty()) {
                    finished = true;
                }
                this.runningProcess = null;
                return;
            }
        }
        this.runningProcess = getActiveArray().getProcess(priority);
        this.runningProcess.setResponseTime(counter);
    }
    private void processWait() {
        for (List<O1Process> process_list : getActiveArray().process_array) {
            for (Process p : process_list) {
                if (p != this.runningProcess) {
                    if (!p.processWait()) {
                        System.err.println("ActiveArray has process finised or doing IO! Process: " + p.toString());
                    }
                }
            }
        }
        for (List<O1Process> process_list : getExpireArray().process_array) {
            for (Process p : process_list) {
                if (p != this.runningProcess) {
                    if (!p.processWait()) {
                        System.err.println("ExpireArrary has process finised or doing IO! Process: " + p.toString());
                    }
                }
            }
        }
    }
    private boolean preemptsProcess() {
        if (this.runningProcess == null ||
                this.getActiveArray().find_priority() < this.runningProcess.getDynamicPriority()) {
            return true;
        }
        return false;
    }
    public void run() {
        if (this.finished) {
            return;
        }
        // Process run 1 time slot.
        if (runningProcess == null ||
                this.runningProcess.needSwitch() ||
                this.runningProcess.requestIO() ||
                runningProcess.isFinished() || preemptsProcess()) {
            switchNextProcess();
        }
        counter += 1;

        // No process to run.
        if (runningProcess == null) {
            return;
        }
        processWait();
        if (this.runningProcess.exec(OperationType.COMPUTE)){
            // Remove this process then add back to the end of process queue.
            this.getActiveArray().removeProcess(this.runningProcess);
            this.getActiveArray().addProcess(this.runningProcess);
            switchNextProcess();
        } else {
            System.out.println("ERROR happens it should never reached here");
        }
    }
    public void IO() {
        if (ioProcessList.isEmpty()) {
            return;
        }
        O1Process p = ioProcessList.get(0);
        if (p.exec(OperationType.IO)) {
            if (!p.requestIO()) {
                p.prioCaluAfterIO(counter);
                ioProcessList.remove(p);
                getExpireArray().addProcess(p);
            }
        }
        else {
            System.out.println("ERROR happens process in IO list should be able to do IO");
        }
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("The active array: \n").append(getActiveArray().toString()).append("\n");
        sb.append("The expire array: \n").append(getExpireArray().toString()).append("\n");
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
        getActiveArray().addProcess(new O1Process(p));
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
            total_weighted_turnaround_time += turnaround * (PrioArray.MAX_PRIORITY - p.priority);
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
        return "O1Scheduler           ";
    }
}

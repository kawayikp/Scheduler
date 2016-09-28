package scheduling;

/**
 * Created by Juntong Liu.
 */
import java.util.List;
public interface Scheduler {
    // a new process come.
    void enqueueProcess(Process p);
    // run one time slot.
    void run();
    // check whether the process is finished.
    boolean finished();
    // do io on one time slot.
    void IO();
    String getName();
    // Return a list of string of the summary result. the sequence should be throughput, avg turn around time,
    // avg wait time, avg response time and total weight turn around time.
    List<String> summary();
}

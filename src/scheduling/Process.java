package scheduling;

/**
 * Created by Juntong Liu.
 */
import java.util.List;

enum OperationType {
    COMPUTE,
    IO
}

class TimeSlice {
    int time;
    OperationType type;
    TimeSlice(int time, OperationType type) {
        this.time = time;
        this.type = type;
    }
}

public abstract class Process {
    int create_time;
    int finish_time;
    int response_time;
    boolean set_response_time;
    int waiting_time;
    int priority;
    List<TimeSlice> timeSlices;
    boolean finished;
    String name;
    public Process(String name, int create_time, int priority, List<TimeSlice> timeSlices) {
        this.name = name;
        this.create_time = create_time;
        this.priority = priority;
        this.timeSlices = timeSlices;
        this.finished = false;
        this.response_time = -1;
        this.waiting_time = 0;
        this.finish_time = -1;
        this.set_response_time = false;
    }
    public boolean isFinished() {
        return this.finished;
    }
    public boolean exec(OperationType type) {
        if (this.isFinished() || (this.timeSlices.get(0).type != type)){
            return false;
        }
        if (this.timeSlices.get(0).time > 0) {
            this.timeSlices.get(0).time -= 1;
        }
        if (this.timeSlices.get(0).time == 0) {
            this.timeSlices.remove(0);
        }
        if (this.timeSlices.isEmpty()) {
            this.finished = true;
        }
        return true;
    }
    public boolean requestIO() {
        if (this.finished) return false;
        return this.timeSlices.get(0).type == OperationType.IO;
    }
    public boolean setFinishTime(int time) {
        if (this.timeSlices.isEmpty()) {
            this.finish_time = time;
        }
        return !this.timeSlices.isEmpty();
    }
    public boolean setResponseTime(int time) {
        if (this.response_time > 0 || this.set_response_time == true) {
            return false;
        }
        this.response_time = time - create_time;
        this.set_response_time = true;
        return true;
    }
    public boolean processWait() {
        if (this.finished || requestIO()) {
            return false;
        } else {
            this.waiting_time += 1;
            return true;
        }
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Process ").append(this.name).append(" created at ").append(this.create_time).
                append(" with priority ").append(this.priority);
        if (this.finished) {
            sb.append(" finished at ").append(this.finish_time);
        } else {
            sb.append(" still running.\n");
            for (TimeSlice ts : this.timeSlices) {
                sb.append(ts.type).append(" ").append(ts.time).append(" ");
            }
        }
        return sb.toString();
    }
}

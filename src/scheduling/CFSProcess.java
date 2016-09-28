package scheduling;

/**
 * Created by Yu Wang.
 */


import java.util.List;

class CFSProcess extends Process implements Comparable<Object>{

    public double vruntime;
    public int weight;
    public int nice_value;
    public static int weight0 = 1024;
    public static int nice_value_high = 19;
    public static int nice_value_low = -20;
    public static int priority_high = 0;
    public static int priority_low = 139;

    public CFSProcess(String name, int create_time, int priority, List<TimeSlice> timeSlices, double vruntime) {
        super(name, create_time, priority, timeSlices);
        this.vruntime = vruntime;
        this.nice_value = (int)((1 - this.priority * 1.0 / (CFSProcess.priority_low - CFSProcess.priority_high + 1))
                * (CFSProcess.nice_value_high - CFSProcess.nice_value_low + 1)) + CFSProcess.nice_value_low;
        this.weight = (int) (1024 / Math.pow(1.25, nice_value));
    }

    public void setUpVruntime(double newTime) {
        this.vruntime = newTime;
    }

    public double getExecTime() {
        return this.vruntime;
    }

    public int compareTo(Object obj){
        CFSProcess st = (CFSProcess)obj;
        if (this.vruntime == st.vruntime) {
            return 0;
        } else if (this.vruntime < st.vruntime) {
            return -1;
        } else {
            return 1;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Process ").append(this.name).append(" created at ").append(this.create_time).
                append(" with priority ").append(this.priority).append(" and vruntime ").append(this.vruntime);
        if (this.finished) {
            sb.append(" finished at ").append(this.finish_time);
        } else {
            sb.append(" still running.\n");
            for (TimeSlice ts : this.timeSlices) {
                sb.append(ts.type).append(" ").append(ts.time).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

}

/**
 * 
 */
package weiboCraw.scheduler;

import java.util.List;

/**
 * @author weibornhigh
 *
 */
public interface IScheduler {
    public static final String WAITING = "waiting";
    public static final String RUNNING = "running";
    public static final String ERROR = "error";
    public static final String UNCOMLETED = "uncompleted";
    public static final String COMPLETED = "completed";
    public static final String UNKNOWN = "unknown";

    public String assignJob();
    /**
     * Mark the job with the id to be complete.
     * @param id The POI_ID/USER_ID being marked.
     * @param error FALSE->ERROR; TRUE->COMPLETE.
     */
    public boolean setJobStatus(String id, String status);
    public String getJobStatus(String id);
    public boolean uncomplete(String id, List<Integer> missedPage);
    public boolean incomplete(String id);
    /**
     * Mark all the Jobs with RUNNING or ERROR status to be WAITING,
     * in order to finish them next time.
     */
    public boolean suspend();
}
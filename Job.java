/**
 * One job started by the shell in the background.
 * Keeps the id we show the user, the real OS pid, and the command line.
 */
public class Job {

    public final int id;      // number shown in "jobs"
    public final long pid;    // real Windows process id
    public final String command;

    public Job(int id, long pid, String command) {
        this.id = id;
        this.pid = pid;
        this.command = command;
    }
}

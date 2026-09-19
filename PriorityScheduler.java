import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Priority scheduler simulation for Deliverable 2.
 *
 * Processes wait in a heap (a Java PriorityQueue) ordered by priority, the
 * highest one first. Ties are broken first-come-first-served with a sequence
 * number. The simulation ticks one virtual second at a time; when a process
 * arrives with a higher priority than the one running, the running process is
 * preempted and pushed back into the heap.
 */
public class PriorityScheduler {

    // one simulated process. A higher priority number means more important.
    private static class SimProcess {
        final String name;
        final int priority;
        final int burst;
        final int arrival;
        final int seq;      // arrival order, breaks priority ties fcfs style
        int remaining;
        int firstRun = -1;  // clock time it first got the cpu
        int finish = -1;    // clock time it completed

        SimProcess(String name, int priority, int burst, int arrival, int seq) {
            this.name = name;
            this.priority = priority;
            this.burst = burst;
            this.arrival = arrival;
            this.seq = seq;
            this.remaining = burst;
        }
    }

    // the demo scenario. P1 starts alone, then more important jobs arrive and
    // preempt it. P2 and P3 share priority 5, so P3 must wait behind P2 (fcfs).
    private static final Object[][] DEMO = {
            {"P1", 1, 6, 0},
            {"P4", 2, 4, 1},
            {"P2", 5, 3, 2},
            {"P3", 5, 2, 3},
    };

    public static void main(String[] args) {
        run();
    }

    // used by the shell's priority command
    public static void run() {
        System.out.println("==================================================");
        System.out.println("  Priority Scheduling - higher number wins");
        System.out.println("  Ties are broken first-come-first-served");
        System.out.println("==================================================");

        // heap ordered by priority (highest first), then by arrival order
        PriorityQueue<SimProcess> heap = new PriorityQueue<>(new Comparator<SimProcess>() {
            public int compare(SimProcess a, SimProcess b) {
                if (a.priority != b.priority) return b.priority - a.priority;
                return a.seq - b.seq;
            }
        });

        List<SimProcess> arrivals = new ArrayList<>();
        for (int i = 0; i < DEMO.length; i++) {
            Object[] row = DEMO[i];
            arrivals.add(new SimProcess((String) row[0], (Integer) row[1],
                    (Integer) row[2], (Integer) row[3], i));
        }

        System.out.print("Processes (name, priority, burst, arrival): ");
        for (SimProcess p : arrivals) {
            System.out.print(p.name + "(prio " + p.priority + ", " + p.burst
                    + " sec, arrives at " + p.arrival + " sec) ");
        }
        System.out.println();
        System.out.println();

        StringBuilder gantt = new StringBuilder();
        List<SimProcess> done = new ArrayList<>();
        SimProcess running = null;
        int clock = 0;

        while (!arrivals.isEmpty() || running != null || !heap.isEmpty()) {
            // anyone arriving right now joins the heap
            for (int i = arrivals.size() - 1; i >= 0; i--) {
                if (arrivals.get(i).arrival == clock) {
                    SimProcess p = arrivals.remove(i);
                    heap.add(p);
                    System.out.println("time " + clock + ": " + p.name
                            + " arrives (priority " + p.priority + ", burst " + p.burst + ")");
                }
            }

            // preempt when a waiting job is more important than the one running
            if (running != null && !heap.isEmpty() && heap.peek().priority > running.priority) {
                System.out.println("time " + clock + ": " + heap.peek().name
                        + " has priority " + heap.peek().priority + ", so " + running.name
                        + " (priority " + running.priority + ") is preempted with "
                        + running.remaining + " sec left");
                heap.add(running);
                running = null;
            }

            if (running == null && !heap.isEmpty()) {
                running = heap.poll();
                if (running.firstRun < 0) running.firstRun = clock;
                System.out.println("time " + clock + ": " + running.name
                        + " starts running (priority " + running.priority + ", "
                        + running.remaining + " sec left)");
            }

            if (running != null) {
                sleepOneSecond();
                gantt.append("|").append(running.name);
                running.remaining--;
                clock++;
                if (running.remaining == 0) {
                    running.finish = clock;
                    done.add(running);
                    System.out.println("time " + clock + ": " + running.name + " finished");
                    running = null;
                    System.out.println();
                }
            } else {
                // nothing to run - just let the clock tick forward
                clock++;
            }
        }

        System.out.println("Gantt chart (one cell per second):");
        System.out.println(gantt.toString());
        System.out.println();
        printMetrics(done);
    }

    // waiting, turnaround and response time for every finished process
    private static void printMetrics(List<SimProcess> done) {
        System.out.println("Performance metrics:");
        System.out.printf("%-9s %-10s %-7s %-9s %-8s %-9s %-12s %-9s%n",
                "Process", "Priority", "Burst", "Arrival", "Finish", "Waiting",
                "Turnaround", "Response");
        int totalWaiting = 0;
        int totalTurnaround = 0;
        int totalResponse = 0;
        for (SimProcess p : done) {
            int waiting = p.finish - p.arrival - p.burst;
            int turnaround = p.finish - p.arrival;
            int response = p.firstRun - p.arrival;
            totalWaiting += waiting;
            totalTurnaround += turnaround;
            totalResponse += response;
            System.out.printf("%-9s %-10d %-7d %-9d %-8d %-9d %-12d %-9d%n",
                    p.name, p.priority, p.burst, p.arrival, p.finish,
                    waiting, turnaround, response);
        }
        int n = done.size();
        System.out.printf("%-9s %-10s %-7s %-9s %-8s %-9.1f %-12.1f %-9.1f%n",
                "Average", "-", "-", "-", "-",
                (double) totalWaiting / n,
                (double) totalTurnaround / n,
                (double) totalResponse / n);
    }

    // the timer: one second of fake cpu time
    private static void sleepOneSecond() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

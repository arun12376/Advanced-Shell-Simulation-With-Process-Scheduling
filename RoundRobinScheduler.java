import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;

/**
 * Round-Robin scheduler simulation for Deliverable 2.
 *
 * Every process gets a time slice called the quantum. When the slice is over
 * the process goes to the back of the queue and the next one runs. A process
 * that finishes before its slice ends is removed from the queue for good.
 * Thread.sleep() is the timer: one sleep is one second of simulated cpu time.
 */
public class RoundRobinScheduler {

    // the demo jobs used when the user does not give their own burst times
    private static final int[] DEFAULT_BURSTS = {5, 3, 6, 2, 4};

    // one simulated process: a name, its burst time, and bookkeeping fields
    private static class SimProcess {
        final String name;
        final int burst;
        final int arrival;
        int remaining;
        int firstRun = -1; // clock time of the first slice it got
        int finish = -1;   // clock time when it completed

        SimProcess(String name, int burst, int arrival) {
            this.name = name;
            this.burst = burst;
            this.arrival = arrival;
            this.remaining = burst;
        }
    }

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int quantum;

        if (args.length >= 1) {
            quantum = parseNumber(args[0], -1);
        } else {
            System.out.print("Enter the time quantum (seconds): ");
            quantum = parseNumber(in.hasNextLine() ? in.nextLine().trim() : "", -1);
        }
        if (quantum <= 0) {
            System.out.println("The quantum must be a positive number.");
            return;
        }

        List<Integer> bursts = new ArrayList<>();
        if (args.length > 1) {
            for (int i = 1; i < args.length; i++) {
                int b = parseNumber(args[i], -1);
                if (b <= 0) {
                    System.out.println("Burst times must be positive numbers.");
                    return;
                }
                bursts.add(b);
            }
        } else {
            for (int b : DEFAULT_BURSTS) bursts.add(b);
        }

        run(quantum, bursts);
    }

    // used by the shell's rr command when the user passes burst times
    public static void run(int quantum, List<Integer> bursts) {
        System.out.println("==================================================");
        System.out.println("  Round-Robin Scheduling - time quantum: " + quantum + " sec");
        System.out.println("==================================================");

        Queue<SimProcess> queue = new LinkedList<>();
        List<SimProcess> done = new ArrayList<>();
        StringBuilder gantt = new StringBuilder();

        int i = 1;
        for (int burst : bursts) {
            queue.add(new SimProcess("P" + i, burst, 0));
            i++;
        }
        System.out.print("Ready queue: ");
        for (SimProcess p : queue) {
            System.out.print(p.name + "(" + p.burst + " sec) ");
        }
        System.out.println();
        System.out.println();

        int clock = 0;
        while (!queue.isEmpty()) {
            SimProcess p = queue.poll();
            if (p.firstRun < 0) p.firstRun = clock;

            int slice = Math.min(quantum, p.remaining);
            System.out.println("time " + clock + ": " + p.name + " gets the cpu for "
                    + slice + " sec (" + p.remaining + " sec still needed)");
            for (int t = 0; t < slice; t++) {
                sleepOneSecond();
                gantt.append("|").append(p.name);
            }
            clock += slice;
            p.remaining -= slice;

            if (p.remaining == 0) {
                p.finish = clock;
                done.add(p);
                System.out.println("time " + clock + ": " + p.name + " finished");
            } else {
                System.out.println("time " + clock + ": quantum over, " + p.name
                        + " goes to the back of the queue with " + p.remaining + " sec left");
                queue.add(p);
            }
            System.out.println();
        }

        System.out.println("Gantt chart (one cell per second):");
        System.out.println(gantt.toString());
        System.out.println();
        printMetrics(done);
    }

    // used by the shell's rr command when only the quantum was given
    public static void run(int quantum) {
        List<Integer> bursts = new ArrayList<>();
        for (int b : DEFAULT_BURSTS) bursts.add(b);
        run(quantum, bursts);
    }

    // waiting, turnaround and response time for every finished process
    private static void printMetrics(List<SimProcess> done) {
        System.out.println("Performance metrics:");
        System.out.printf("%-9s %-7s %-7s %-8s %-12s %-9s%n",
                "Process", "Burst", "Finish", "Waiting", "Turnaround", "Response");
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
            System.out.printf("%-9s %-7d %-7d %-8d %-12d %-9d%n",
                    p.name, p.burst, p.finish, waiting, turnaround, response);
        }
        int n = done.size();
        System.out.printf("%-9s %-7s %-7s %-8.1f %-12.1f %-9.1f%n",
                "Average", "-", "-",
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

    private static int parseNumber(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}

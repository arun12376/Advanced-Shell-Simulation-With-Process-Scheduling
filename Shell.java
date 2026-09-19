import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Mini-OS Shell - Deliverable 1
 *
 * A small shell that simulates a Unix-like environment on top of Windows.
 * Built-in commands (cd, ls, cat, ...) are handled by the shell itself,
 * everything else is handed to the real OS. Background jobs are tracked
 * in a job list so the user can check them (jobs), wait for them (fg),
 * and terminate them (kill).
 */
public class Shell {

    // background jobs started by this shell, and the id for the next one
    private static final List<Job> jobs = new ArrayList<>();
    private static int nextJobId = 1;

    // the directory the shell is currently looking at
    private static File currentDir = new File(System.getProperty("user.dir"));

    public static void main(String[] args) {
        printWelcome();

        Scanner in = new Scanner(System.in);
        while (true) {
            reapFinishedJobs(); // report jobs that ended on their own

            System.out.print("myshell:" + currentDir.getName() + "> ");
            System.out.flush(); // make sure the prompt shows up right away
            if (!in.hasNextLine()) break; // input closed (Ctrl+D)
            String line = in.nextLine().trim();
            if (line.isEmpty()) continue;

            List<String> tokens = tokenize(line);
            boolean background = isBackgroundRequest(tokens);
            if (tokens.isEmpty()) continue;

            String command = tokens.get(0);
            List<String> cmdArgs = tokens.subList(1, tokens.size());

            if (!runBuiltin(command, cmdArgs)) {
                runExternal(command, cmdArgs, background);
            }
        }
        System.out.println("\nbye");
    }

    // ---------------------------------------------------------------
    // parsing helpers
    // ---------------------------------------------------------------

    // splits a line into words, keeping anything between quotes as one word
    private static List<String> tokenize(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) tokens.add(current.toString());
        return tokens;
    }

    // a line that ends with "&" asks the shell to run the job in the background
    private static boolean isBackgroundRequest(List<String> tokens) {
        if (tokens.get(tokens.size() - 1).equals("&")) {
            tokens.remove(tokens.size() - 1);
            return true;
        }
        return false;
    }

    // returns the job the user asked for, or the newest one if no id was given
    private static Job pickJob(List<String> args, String who) {
        if (args.isEmpty()) {
            if (jobs.isEmpty()) {
                System.out.println(who + ": no background jobs");
                return null;
            }
            return jobs.get(jobs.size() - 1);
        }
        int id;
        try {
            id = Integer.parseInt(args.get(0));
        } catch (NumberFormatException e) {
            System.out.println(who + ": '" + args.get(0) + "' is not a job id");
            return null;
        }
        for (Job j : jobs) {
            if (j.id == id) return j;
        }
        System.out.println(who + ": job " + id + " not found");
        return null;
    }

    // ---------------------------------------------------------------
    // built-in commands - each one returns true when it handled the line
    // ---------------------------------------------------------------

    private static boolean runBuiltin(String cmd, List<String> args) {
        switch (cmd) {
            case "cd":    return cd(args);
            case "pwd":   return pwd(args);
            case "exit":  exitShell(); return true;
            case "echo":  return echo(args);
            case "clear": return clear(args);
            case "ls":    return ls(args);
            case "cat":   return cat(args);
            case "mkdir": return mkdir(args);
            case "rmdir": return rmdir(args);
            case "rm":    return rm(args);
            case "touch": return touch(args);
            case "kill":  return kill(args);
            case "jobs":  return listJobs(args);
            case "fg":    return fg(args);
            case "bg":    return bg(args);
            case "rr":    return rr(args);
            case "priority": return priority(args);
            case "help":  return help(args);
            default:      return false;
        }
    }

    // cd [dir] - move to another directory, or to home when no dir is given
    private static boolean cd(List<String> args) {
        String target = args.isEmpty() ? System.getProperty("user.home") : args.get(0);
        if (target.equals("~")) target = System.getProperty("user.home");

        File dir = new File(target);
        if (!dir.isAbsolute()) dir = new File(currentDir, target);

        if (!dir.isDirectory()) {
            System.out.println("cd: " + target + ": no such directory");
        } else {
            try {
                currentDir = dir.getCanonicalFile(); // resolves ".." so the prompt stays clean
            } catch (IOException e) {
                currentDir = dir;
            }
        }
        return true;
    }

    // pwd - print the current directory (using / instead of \ like Unix)
    private static boolean pwd(List<String> args) {
        System.out.println(currentDir.getPath().replace('\\', '/'));
        return true;
    }

    // echo [text] - print the words back to the screen
    private static boolean echo(List<String> args) {
        System.out.println(String.join(" ", args));
        return true;
    }

    // clear - wipe the terminal screen using an ANSI escape sequence
    private static boolean clear(List<String> args) {
        System.out.print("cleared");
        System.out.flush();
        return true;
    }

    // ls [dir] - list the files in a directory, folders get a "/" suffix
    private static boolean ls(List<String> args) {
        File dir = currentDir;
        if (!args.isEmpty()) {
            dir = new File(currentDir, args.get(0));
            if (!dir.isDirectory()) {
                System.out.println("ls: " + args.get(0) + ": no such directory");
                return true;
            }
        }
        File[] files = dir.listFiles();
        if (files == null) return true;

        Arrays.sort(files);
        for (File f : files) {
            System.out.println(f.getName() + (f.isDirectory() ? "/" : ""));
        }
        return true;
    }

    // cat [file] - print the contents of one or more files
    private static boolean cat(List<String> args) {
        for (String name : args) {
            File f = new File(currentDir, name);
            if (!f.exists() || f.isDirectory()) {
                System.out.println("cat: " + name + ": no such file");
                continue;
            }
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) System.out.println(line);
            } catch (IOException e) {
                System.out.println("cat: " + name + ": " + e.getMessage());
            }
        }
        return true;
    }

    // mkdir [dir] - create one or more directories
    private static boolean mkdir(List<String> args) {
        for (String name : args) {
            File dir = new File(currentDir, name);
            if (dir.mkdir()) {
                System.out.println("mkdir: created " + name);
            } else {
                System.out.println("mkdir: cannot create " + name);
            }
        }
        return true;
    }

    // rmdir [dir] - remove a directory, works only when it is empty
    private static boolean rmdir(List<String> args) {
        for (String name : args) {
            File dir = new File(currentDir, name);
            if (dir.delete()) {
                System.out.println("rmdir: removed " + name);
            } else {
                System.out.println("rmdir: cannot remove " + name + " (not empty or missing)");
            }
        }
        return true;
    }

    // rm [file] - delete one or more files
    private static boolean rm(List<String> args) {
        for (String name : args) {
            File f = new File(currentDir, name);
            if (f.isDirectory()) {
                System.out.println("rm: " + name + ": is a directory");
            } else if (f.delete()) {
                System.out.println("rm: removed " + name);
            } else {
                System.out.println("rm: cannot remove " + name);
            }
        }
        return true;
    }

    // touch [file] - create an empty file, or refresh the timestamp if it exists
    private static boolean touch(List<String> args) {
        for (String name : args) {
            File f = new File(currentDir, name);
            try {
                if (f.createNewFile()) {
                    System.out.println("touch: created " + name);
                } else {
                    f.setLastModified(System.currentTimeMillis());
                    System.out.println("touch: updated " + name);
                }
            } catch (IOException e) {
                System.out.println("touch: cannot create " + name);
            }
        }
        return true;
    }

    // ---------------------------------------------------------------
    // job control
    // ---------------------------------------------------------------

    // jobs - list every background job with its state
    private static boolean listJobs(List<String> args) {
        if (jobs.isEmpty()) {
            System.out.println("No background jobs.");
            return true;
        }
        for (Job j : jobs) {
            String state = isAlive(j.pid) ? "Running" : "Done";
            System.out.println("[" + j.id + "] " + state + "   pid " + j.pid + "   " + j.command);
        }
        return true;
    }

    // kill <pid> - terminate a process. "kill %2" kills background job 2.
    private static boolean kill(List<String> args) {
        if (args.isEmpty()) {
            System.out.println("kill: missing pid");
            return true;
        }
        long pid;
        Job job = null;

        if (args.get(0).startsWith("%")) {
            // %<id> means "the job with this id"
            job = pickJob(Arrays.asList(args.get(0).substring(1)), "kill");
            if (job == null) return true;
            pid = job.pid;
        } else {
            try {
                pid = Long.parseLong(args.get(0));
            } catch (NumberFormatException e) {
                System.out.println("kill: '" + args.get(0) + "' is not a valid pid");
                return true;
            }
            for (Job j : jobs) {
                if (j.pid == pid) job = j;
            }
        }

        try {
            Process p = new ProcessBuilder("taskkill", "/pid", Long.toString(pid), "/f")
                    .redirectErrorStream(true).start();
            readWithTimeout(p, 10);
            p.waitFor();

            if (p.exitValue() != 0) {
                if (job != null) {
                    jobs.remove(job);
                    System.out.println("kill: job [" + job.id + "] was already finished");
                } else {
                    System.out.println("kill: no process with pid " + pid);
                }
            } else {
                String note = job != null ? " (job [" + job.id + "])" : "";
                System.out.println("Terminated process " + pid + note);
                if (job != null) jobs.remove(job);
            }
        } catch (Exception e) {
            System.out.println("kill: failed (" + e.getMessage() + ")");
        }
        return true;
    }

    // fg [job id] - wait in the foreground until a background job finishes
    private static boolean fg(List<String> args) {
        Job job = pickJob(args, "fg");
        if (job == null) return true;

        System.out.println("fg: waiting for [" + job.id + "] " + job.command);
        while (isAlive(job.pid)) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("[" + job.id + "]+ Done      " + job.command);
        jobs.remove(job);
        return true;
    }

    // bg [job id] - report a background job's state. Windows gives us no
    // way to pause a child process, so jobs keep running on their own.
    private static boolean bg(List<String> args) {
        Job job = pickJob(args, "bg");
        if (job == null) return true;

        if (isAlive(job.pid)) {
            System.out.println("[" + job.id + "] running in the background: " + job.command);
        } else {
            System.out.println("[" + job.id + "] has already finished");
            jobs.remove(job);
        }
        return true;
    }

    // called before every prompt: removes jobs that finished on their own
    private static void reapFinishedJobs() {
        Iterator<Job> it = jobs.iterator();
        while (it.hasNext()) {
            Job job = it.next();
            if (!isAlive(job.pid)) {
                System.out.println("[" + job.id + "]+ Done      " + job.command);
                it.remove();
            }
        }
    }

    // asks Windows whether a pid is still running
    private static boolean isAlive(long pid) {
        try {
            Process p = new ProcessBuilder("tasklist", "/fi", "PID eq " + pid, "/nh")
                    .redirectErrorStream(true).start();
            String out = readWithTimeout(p, 10);
            p.waitFor();
            return out.contains(" " + pid + " ");
        } catch (Exception e) {
            return false;
        }
    }

    // ---------------------------------------------------------------
    // external commands
    // ---------------------------------------------------------------

    // hands a command to the real OS. Foreground jobs block the shell,
    // background jobs are started by startBackgroundJob() instead.
    private static void runExternal(String command, List<String> args, boolean background) {
        if (background) {
            startBackgroundJob(command, args);
            return;
        }

        List<String> full = new ArrayList<>();
        full.add(command);
        full.addAll(args);

        try {
            ProcessBuilder pb = new ProcessBuilder(full);
            pb.directory(currentDir);
            pb.inheritIO(); // the program shares the terminal with the shell
            int code = pb.start().waitFor();
            if (code != 0) {
                System.out.println(command + ": exited with code " + code);
            }
        } catch (IOException e) {
            // no program with that name - let cmd.exe try, so Windows
            // commands like "dir" or "type" also work
            try {
                ProcessBuilder pb = new ProcessBuilder("cmd", "/c", command + " " + String.join(" ", args));
                pb.directory(currentDir);
                pb.inheritIO();
                pb.start().waitFor();
            } catch (Exception e2) {
                System.out.println("myshell: '" + command + "': command not found");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // runs a command in the background and records it as a job.
    // Java 8 does not expose the pid of a child process, so we start the
    // program through PowerShell, which prints the real pid for us.
    private static void startBackgroundJob(String command, List<String> args) {
        String script = "$p = Start-Process -FilePath " + quote(command);
        if (!args.isEmpty()) {
            String argList = args.stream().map(Shell::quote).collect(Collectors.joining(","));
            script += " -ArgumentList " + argList;
        }
        script += " -PassThru -NoNewWindow; $p.Id";

        try {
            ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-Command", script);
            pb.directory(currentDir);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String output = readWithTimeout(p, 10);
            p.waitFor();

            long pid = parsePid(output);
            if (pid < 0) {
                System.out.println("myshell: could not start '" + command + "'");
                return;
            }
            Job job = new Job(nextJobId++, pid, command + " " + String.join(" ", args));
            jobs.add(job);
            System.out.println("[" + job.id + "] " + job.pid);
        } catch (Exception e) {
            System.out.println("myshell: could not start '" + command + "' (" + e.getMessage() + ")");
        }
    }

    // ---------------------------------------------------------------
    // scheduling simulations (Deliverable 2)
    // ---------------------------------------------------------------

    // rr <quantum> [burst...] - run the round-robin scheduler simulation.
    // Without burst times the default demo jobs are used.
    private static boolean rr(List<String> args) {
        if (args.isEmpty()) {
            System.out.println("rr: usage: rr <quantum> [burst time of each process]");
            return true;
        }

        int quantum;
        try {
            quantum = Integer.parseInt(args.get(0));
        } catch (NumberFormatException e) {
            System.out.println("rr: '" + args.get(0) + "' is not a number");
            return true;
        }
        if (quantum <= 0) {
            System.out.println("rr: the quantum must be a positive number");
            return true;
        }

        List<Integer> bursts = new ArrayList<>();
        for (int i = 1; i < args.size(); i++) {
            try {
                bursts.add(Integer.parseInt(args.get(i)));
            } catch (NumberFormatException e) {
                System.out.println("rr: '" + args.get(i) + "' is not a number");
                return true;
            }
        }

        if (bursts.isEmpty()) {
            RoundRobinScheduler.run(quantum);
        } else {
            RoundRobinScheduler.run(quantum, bursts);
        }
        return true;
    }

    // priority - run the priority scheduler simulation with preemption
    private static boolean priority(List<String> args) {
        PriorityScheduler.run();
        return true;
    }

    // ---------------------------------------------------------------
    // small helpers
    // ---------------------------------------------------------------

    // reads a process output with a timeout so the shell never hangs
    private static String readWithTimeout(Process p, int seconds) {
        StringBuilder sb = new StringBuilder();
        Thread reader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append("\n");
            } catch (IOException ignored) {
            }
        });
        reader.setDaemon(true);
        reader.start();
        try {
            reader.join(seconds * 1000);
        } catch (InterruptedException ignored) {
        }
        return sb.toString();
    }

    // wraps a string in single quotes for PowerShell ('' escapes a quote)
    private static String quote(String s) {
        return "'" + s.replace("'", "''") + "'";
    }

    // the powershell wrapper prints the new pid as the last line
    private static long parsePid(String output) {
        for (String line : output.split("\\R")) {
            line = line.trim();
            if (line.matches("\\d+")) return Long.parseLong(line);
        }
        return -1;
    }

    private static void exitShell() {
        System.out.println("bye");
        System.exit(0);
    }

    private static void printWelcome() {
        System.out.println("============================================");
        System.out.println("  Mini-OS Shell - Deliverable 2");
        System.out.println("  Type \"help\" to see the built-in commands.");
        System.out.println("  Type \"exit\" to quit.");
        System.out.println("============================================");
    }

    // help - a quick list of what the shell can do
    private static boolean help(List<String> args) {
        System.out.println();
        System.out.println("Built-in commands:");
        System.out.println("  cd <dir>        change the current directory");
        System.out.println("  pwd             print the current directory");
        System.out.println("  echo <text>     print text to the screen");
        System.out.println("  clear           clear the screen");
        System.out.println("  ls [dir]        list files in a directory");
        System.out.println("  cat <file>      show the contents of a file");
        System.out.println("  mkdir <dir>     create a directory");
        System.out.println("  rmdir <dir>     remove an empty directory");
        System.out.println("  rm <file>       delete a file");
        System.out.println("  touch <file>    create a file or refresh its timestamp");
        System.out.println("  kill <pid>      terminate a process");
        System.out.println("  jobs            list background jobs");
        System.out.println("  fg [job id]     wait for a background job to finish");
        System.out.println("  bg [job id]     show the state of a background job");
        System.out.println("  rr <quantum> [bursts]  run the round-robin scheduling simulation");
        System.out.println("  priority        run the priority scheduling simulation");
        System.out.println("  exit            leave the shell");
        System.out.println();
        System.out.println("Anything else is run as an external program.");
        System.out.println("Add & at the end to run a command in the background.");
        System.out.println("kill %<job id> terminates a job by its job number.");
        System.out.println();
        return true;
    }
}

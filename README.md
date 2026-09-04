# Advanced Shell Simulation with Integrated OS Concepts

A mini Unix-like shell written in Java that runs on Windows. It has built-in commands (cd, pwd, ls, cat, mkdir, rm, ...), hands everything else to the real OS, and tracks background jobs with jobs, fg, bg, and kill.

## How to run

You need Java 8 or newer.

1. Open a terminal in this folder.
2. Compile:

       javac Shell.java Job.java

3. Run:

       java Shell

Type `help` to see the built-in commands and `exit` to quit. Add `&` at the end of a command to run it in the background.

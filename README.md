# Advanced Shell Simulation With Process Scheduling

Four Java classes: a mini shell with built-in commands and job control, plus two process scheduling simulations.

- Shell.java - the shell itself, includes the rr and priority built-in commands
- Job.java - one background job tracked by the shell
- RoundRobinScheduler.java - round-robin scheduling with a configurable time quantum
- PriorityScheduler.java - priority scheduling with a heap and preemption

## How to run

Compile all files:

    javac Shell.java Job.java RoundRobinScheduler.java PriorityScheduler.java

Run the shell (type help for the command list, rr 2 for round-robin, priority for priority scheduling):

    java Shell

Or run a scheduler on its own:

    java RoundRobinScheduler 2

    java PriorityScheduler

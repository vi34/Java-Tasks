package ru.ifmo.ctddev.shatrov.parallelism;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Created by vi34 on 26.03.15.
 */
public class TaskQueue {
    private Queue<Task<?,?> > taskQueue;

    TaskQueue() {
        taskQueue = new LinkedList<>();
    }

    public synchronized void addTask(Task task) {
        taskQueue.add(task);
        notifyAll();
    }

    synchronized Task getTask() throws InterruptedException {
        while (taskQueue.isEmpty()) {
            wait();
        }
        return taskQueue.poll();
    }


}

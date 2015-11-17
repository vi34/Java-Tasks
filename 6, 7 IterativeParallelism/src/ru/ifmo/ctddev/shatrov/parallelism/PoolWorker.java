package ru.ifmo.ctddev.shatrov.parallelism;

import java.util.function.Function;

/**
 * Created by vi34 on 26.03.15.
 */
public class PoolWorker implements Runnable {

    TaskQueue queue;

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                Task task = queue.getTask();
                synchronized (task) {
                    task.doTask();
                    task.notify();
                }
            }
        } catch (InterruptedException e) { }
        Thread.currentThread().interrupt();
    }

}

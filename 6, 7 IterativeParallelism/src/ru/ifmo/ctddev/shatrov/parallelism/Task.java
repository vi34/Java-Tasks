package ru.ifmo.ctddev.shatrov.parallelism;

import java.util.function.Function;

/**
 * Created by vi34 on 26.03.15.
 */
public class Task<T, R> {
    private Function<? super T, ? extends R> function;
    private T arg;
    private R res;

    public void setTask(Function<? super T, ? extends R> function, T arg) {
        this.function = function;
        this.arg = arg;
        res = null;
    }

    public void doTask() {
       res = function.apply(arg);
    }

    public synchronized R getResult() throws InterruptedException {
        while (res == null) {
            wait();
        }
        return res;
    }
}

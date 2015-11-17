package ru.ifmo.ctddev.shatrov.parallelism;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Created by vi34 on 26.03.15.
 */
public class ParallelMapperImpl implements ParallelMapper{

    private List<Thread> pool;
    private TaskQueue taskQueue;

    /**
     * Construct ParallelMapper with given number of threads
     * @param threadsCnt number of threads that mapper can use
     */

    public ParallelMapperImpl(int threadsCnt) {
        pool = new ArrayList<Thread>();
        taskQueue = new TaskQueue();
        for(int i = 0; i < threadsCnt; ++i) {
            PoolWorker worker = new PoolWorker();
            worker.queue = taskQueue;
            Thread thread = new Thread(worker);
            pool.add(thread);
            thread.start();
        }
    }

    /**
     * Apply given <tt>function </tt> to given list of arguments and work in parallel in number of given threads
     * @param function function we need to apply
     * @param list list of arguments to apply funtion for
     * @param <T> type of elements in list
     * @param <R> result type of elements in list
     * @return list of arguments - result of applying function
     * @throws InterruptedException if any worker thread was interrupted
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> function, List<? extends T> list) throws InterruptedException {
        List<Task<T, R>> taskList = new ArrayList<>();
        List<R> result = new ArrayList<>();
        for (T arg: list) {
            Task<T, R> task = new Task<>();
            taskList.add(task);
            task.setTask(function, arg);
            taskQueue.addTask(task);
        }
        for (Task<T,R> task: taskList) {
            result.add(task.getResult());
        }
        return result;
    }

    /**
     * Interrupt all working threads
     *
     * @throws InterruptedException if any worker thread was interrupted
     */

    @Override
    public void close() throws InterruptedException {
        boolean interrupt = false;
        for(Thread thread : pool) {
            thread.interrupt();
        }
    }
}

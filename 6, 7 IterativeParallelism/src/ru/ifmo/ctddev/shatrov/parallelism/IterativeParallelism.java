package ru.ifmo.ctddev.shatrov.parallelism;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * @author vi34
 * @version 1.0
 */
public class IterativeParallelism implements ListIP {

    /**
     * Instance of {@code ParallelMapper} to use
     */
    private ParallelMapper mapper;

    public static void main(String[] args) {
        System.out.println("PROC : " + Runtime.getRuntime().availableProcessors());

        ParallelMapper parallelMapper = new ParallelMapperImpl(10);
        IterativeParallelism iP = new IterativeParallelism(parallelMapper);
        List<Integer> arrayList = new ArrayList<>();
        for (int i = 1; i <= 100; ++i) {
            arrayList.add(i);
        }
        try {
            arrayList = iP.<Integer, Integer>map(10, arrayList, (n) -> (-1 * n + 1));
            //arrayList = parallelMapper.<Integer, Integer>map((n) -> -1 * n, arrayList);
            for (Integer integer : arrayList) {
                System.out.println(integer);
            }
            parallelMapper.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create a simple instance that creates and manages <tt>Threads</tt> by itself
     */
    public IterativeParallelism() {
    }

    /**
     * Create instance that will use given {@link ParallelMapper} to perform all actions.
     *
     * @param mapper ParallelMapper to perform actions
     * @see ParallelMapper
     */
    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Instances of this class performs heavy operations.
     *
     * @param <T> type of result
     */
    private abstract class Worker<T> implements Runnable {
        private T result;

        public void setResult(T result) {
            this.result = result;
        }

        public T getResult() {
            return result;
        }
    }


    /**
     * Performs given function parallel on a <tt>list</tt>.
     * <p> Divides <tt>list</tt> on parts using {@link #makePartitions(List, int)}.
     * If {@link #mapper} is set, will use it to perform function else creates threads and performs given function
     * in them on each part. Finally combines the result with <tt>merge</tt> function.
     * </p>
     *
     * @param threadsCnt number of threads that can be used
     * @param list       List on which function must be performed
     * @param function   function to perform
     * @param merge      function to combine result
     * @param <T>        type of elements in List
     * @param <R>        result type
     * @return result of the function on each part of list combined by <tt>merge</tt>
     * @throws InterruptedException if any worker thread was interrupted
     */
    private <T, R> R makeParallel(int threadsCnt, List<? extends T> list,
                                  Function<List<? extends T>, R> function, Function<List<R>, R> merge) throws InterruptedException {
        List<List<? extends T>> parts = makePartitions(list, threadsCnt);
        List<R> mergeResult = new ArrayList<>();
        if (mapper == null) {
            List<Worker<R>> workers = new ArrayList<>();
            List<Thread> threads = new ArrayList<>();
            for (List<? extends T> part : parts) {
                Worker<R> worker = new Worker<R>() {
                    @Override
                    public void run() {
                        setResult(function.apply(part));
                    }
                };
                workers.add(worker);
                Thread thread = new Thread(worker);
                threads.add(thread);
                thread.start();
            }

            for (int i = 0; i < threads.size(); ++i) {
                threads.get(i).join();
                mergeResult.add(workers.get(i).getResult());
            }
        } else {
            mergeResult = mapper.map(function, parts);
        }

        return merge.apply(mergeResult);
    }

    /**
     * Splits given list on given number of parts
     *
     * @param list  list to split
     * @param parts number of parts
     * @param <T>   type of elements in list
     * @return List of parts of given list
     */
    private <T> List<List<? extends T>> makePartitions(List<? extends T> list, int parts) {
        if (parts > list.size()) {
            parts = list.size();
        } else if (parts == 0) {
            parts = 0;
        }
        List<List<? extends T>> partitions = new ArrayList<>();
        int prevInd = 0;
        int cur = 0;
        for (int i = 0; i < parts; ++i) {
            cur = (i == parts - 1) ? list.size() : (list.size() / parts) * (i + 1);
            partitions.add(list.subList(prevInd, cur));
            prevInd = cur;
        }
        return partitions;
    }

    /**
     * Returns concatenated <tt>String</tt> representations of all elements in given list
     * <p>Uses {@link #makeParallel(int, List, Function, Function)} to perform whole task parallel</p>
     *
     * @param threads number of <tt>Threads</tt> that can be used
     * @param list    List of elements
     * @return concatenated <tt>String</tt> representations of all elements in given list
     * @throws InterruptedException if any worker thread was interrupted during its work
     * @see Thread
     */
    @Override
    public String concat(int threads, List<?> list) throws InterruptedException {
        return makeParallel(threads, list,
                part -> part.stream().map(Object::toString).reduce("", String::concat),
                partsRes -> partsRes.stream().reduce("", String::concat));
    }

    /**
     * Returns <tt>List</tt> which contains only elements that match given predicate.
     * <p>Uses {@link #makeParallel(int, List, Function, Function)} to perform whole task parallel</p>
     *
     * @param threads   number of <tt>Threads</tt> that can be used
     * @param list      initial List of elements
     * @param predicate predicate to determine which elements must be included.
     * @param <T>       type of <tt>list</tt> elements
     * @return <tt>List</tt> which contains only elements that match given predicate.
     * @throws InterruptedException if any worker thread was interrupted during its work
     * @see Thread
     */
    @Override
    public <T> List<T> filter(int threads, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return makeParallel(threads, list,
                part -> part.stream().filter(predicate).collect(Collectors.<T>toList()),
                partsRes -> partsRes.stream().reduce(new ArrayList<T>(), (x, y) -> {
                    x.addAll(y);
                    return x;
                }));
    }

    /**
     * Applies given function on a list.
     * <p>Returns another list where each element is result of function applied to element in initial list</p>
     * <p>Uses {@link #makeParallel(int, List, Function, Function)} to perform whole task parallel</p>
     *
     * @param threads  number of <tt>Threads</tt> that can be used
     * @param list     initial List of elements
     * @param function function to apply
     * @param <T>      type of elements in given list
     * @param <U>      type of elements in a result list
     * @return list of elements with applied function
     * @throws InterruptedException if any worker thread was interrupted during its work
     * @see Thread
     */
    @Override
    public <T, U> List<U> map(int threads, List<? extends T> list, Function<? super T, ? extends U> function) throws InterruptedException {
        return makeParallel(threads, list,
                part -> part.stream().map(function).collect(Collectors.toList()),
                partsRes -> partsRes.stream().reduce(new ArrayList<U>(), (x, y) -> {
                    x.addAll(y);
                    return x;
                }));
    }

    /**
     * For the given <tt>list</tt> returns maximum element in this list according to
     * <tt>comparator</tt>.
     * <p>Uses {@link #makeParallel(int, List, Function, Function)} to perform whole task parallel</p>
     *
     * @param threads    number of <tt>Threads</tt> that can be used.
     * @param list       <tt>List</tt> where to find minimum element
     * @param comparator comparator according to which compares performs
     * @param <T>        type of elements in a list
     * @return maximum element in list in the sense of <tt>comparator</tt>
     * @throws InterruptedException if any worker thread was interrupted during its work
     * @see Thread
     */
    @Override
    public <T> T maximum(int threads, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        return makeParallel(threads, list, part -> Collections.max(part, comparator), partsRes -> Collections.max(partsRes, comparator));
    }

    /**
     * For the given <tt>list</tt> returns minimum element in this list according to
     * <tt>comparator</tt>.
     * <p>Uses {@link #makeParallel(int, List, Function, Function)} to perform whole task parallel</p>
     *
     * @param threads    number of <tt>Threads</tt> that can be used.
     * @param list       <tt>List</tt> where to find minimum element
     * @param comparator comparator according to which compares performs
     * @param <T>        type of elements in a list
     * @return maximum element in list in the sense of <tt>comparator</tt>
     * @throws InterruptedException if any worker thread was interrupted during its work
     * @see Thread
     */
    @Override
    public <T> T minimum(int threads, List<? extends T> list, Comparator<? super T> comparator) throws InterruptedException {
        return makeParallel(threads, list, part -> Collections.min(part, comparator), partsRes -> Collections.min(partsRes, comparator));
    }

    /**
     * For the given <tt>list</tt> returns true if all elements match given predicate
     * <p>Uses {@link #makeParallel(int, List, Function, Function)} to perform whole task parallel</p>
     *
     * @param threads   number of <tt>Threads</tt> that can be used.
     * @param list      <tt>List</tt> where to find minimum element
     * @param predicate comparator according to which compares performs
     * @param <T>       type of elements in a list
     * @return true if all elements match given predicate
     * @throws InterruptedException if any worker thread was interrupted during its work
     * @see Thread
     */
    @Override
    public <T> boolean all(int threads, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return makeParallel(threads, list, part -> part.stream().allMatch(predicate), (List<Boolean> partsRes) -> partsRes.stream().allMatch((Boolean b) -> b));
    }

    /**
     * For the given <tt>list</tt> returns true if list contains at least one element matching given predicate
     * <p>Uses {@link #makeParallel(int, List, Function, Function)} to perform whole task parallel</p>
     *
     * @param threads   number of <tt>Threads</tt> that can be used.
     * @param list      <tt>List</tt> where to find minimum element
     * @param predicate comparator according to which compares performs
     * @param <T>       type of elements in a list
     * @return true if list contains at least one element matching given predicate
     * @throws InterruptedException if any worker thread was interrupted during its work
     * @see Thread
     */
    @Override
    public <T> boolean any(int threads, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return makeParallel(threads, list, part -> part.stream().anyMatch(predicate), (List<Boolean> partsRes) -> partsRes.stream().anyMatch((Boolean b) -> b));
    }
}

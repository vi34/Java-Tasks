package ru.ifmo.ctddev.shatrov.webcrawler;

import info.kgeorgiy.java.advanced.crawler.Downloader;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by vi34 on 18/05/15.
 */
public class ExecutionContext {
    int maxDepth;
    Lock lock;
    Condition condition;
    ExecutorService downloaders;
    ExecutorService executors;
    ConcurrentSkipListSet<String> visited;
    ConcurrentHashMap<String, Semaphore> hosts;
    Map<String, IOException> errors;
    AtomicInteger tasksNum;
    int perHost;
    Downloader downloader;

    public ExecutionContext(int maxDepth, ExecutorService downloaders,
                            ExecutorService executors, int perHost, Downloader downloader, ConcurrentHashMap<String, Semaphore> hosts) {
        this.maxDepth = maxDepth;
        this.downloaders = downloaders;
        this.executors = executors;
        this.perHost = perHost;
        this.downloader = downloader;
        lock = new ReentrantLock();
        condition = lock.newCondition();
        visited = new ConcurrentSkipListSet<>();
        this.hosts = hosts;
        tasksNum = new AtomicInteger(0);
        errors = new ConcurrentHashMap<>();

    }
}

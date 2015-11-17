package ru.ifmo.ctddev.shatrov.webcrawler;

import info.kgeorgiy.java.advanced.crawler.Document;
import info.kgeorgiy.java.advanced.crawler.Downloader;

import java.io.IOException;
import java.util.List;

/**
 * Created by vi34 on 18/05/15.
 */
public class ExecuteWorker implements Runnable {
    private Document document;
    private int depth;
    private volatile ExecutionContext executionContext;


    ExecuteWorker(Document document, int depth, ExecutionContext executionContext) {
        this.document = document;
        this.depth = depth;
        this.executionContext = executionContext;

    }

    @Override
    public void run() {
        List<String> links;
        try {
            links = document.extractLinks();
            if(depth < executionContext.maxDepth) {
                for (String link : links) {
                    if (executionContext.visited.add(link)) {
                        executionContext.tasksNum.incrementAndGet();
                        executionContext.downloaders.submit(new DownloadWorker(link, depth + 1, executionContext));
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (executionContext.tasksNum.decrementAndGet() == 0) {
                executionContext.lock.lock();
                executionContext.condition.signalAll();
                executionContext.lock.unlock();
            }
        }
    }
}

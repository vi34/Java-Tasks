package ru.ifmo.ctddev.shatrov.webcrawler;

import info.kgeorgiy.java.advanced.crawler.Document;
import info.kgeorgiy.java.advanced.crawler.URLUtils;

import java.io.IOException;
import java.util.concurrent.Semaphore;

/**
 * Created by vi34 on 18/05/15.
 */
public class DownloadWorker implements Runnable {
    private String url;
    private int depth;
    private volatile ExecutionContext executionContext;

    DownloadWorker(String url, int depth,ExecutionContext executionContext) {
        this.url = url;
        this.depth = depth;
        this.executionContext = executionContext;
    }

    @Override
    public void run() {
        Document document;
        String host = null;
        try {
            host = URLUtils.getHost(url);
            Semaphore semaphore = executionContext.hosts.get(host);
            if (semaphore == null) {
                semaphore = new Semaphore(executionContext.perHost);
                executionContext.hosts.put(host, semaphore);
            }

            executionContext.hosts.get(host).acquire();
                document = executionContext.downloader.download(url);
                executionContext.tasksNum.incrementAndGet();
                executionContext.executors.submit(new ExecuteWorker(document,depth,executionContext));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            executionContext.errors.put(url,e);
        } finally {
            if (executionContext.tasksNum.decrementAndGet() == 0) {
                executionContext.lock.lock();
                executionContext.condition.signalAll();
                executionContext.lock.unlock();
            }
            if (host != null) {
                executionContext.hosts.get(host).release();
            }
        }
    }
}

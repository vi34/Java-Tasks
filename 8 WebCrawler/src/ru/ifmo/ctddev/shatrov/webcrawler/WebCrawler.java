package ru.ifmo.ctddev.shatrov.webcrawler;

import info.kgeorgiy.java.advanced.crawler.*;


import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

/**
 * Created by vi34 on 08.04.15.
 */
public class WebCrawler implements Crawler {
    private ExecutorService downloaders;
    private ExecutorService executors;
    ConcurrentHashMap<String, Semaphore> hosts;
    private int perHost;
    private Downloader downloader;

    /**
     * Constructor from given arguments
     *
     * @param downloader  - implements interface {@link Downloader}
     * @param downloaders number of threads that can be used to download web pages
     * @param extractors  number of threads that can be used to extract lonks from documents
     * @param perHost     maximum number of threads that can download from
     */
    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloaders = Executors.newFixedThreadPool(downloaders);
        this.executors = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
        hosts = new ConcurrentHashMap<>();
    }

    public static void main(String[] args) {
        if (args != null && args.length > 0 && args[0] != null) {
            String url = args[0];
            int downloaders = 1;
            int extractors = 1;
            int perHost = 1;
            if (args.length > 1 && args[1] != null) {
                downloaders = Integer.parseInt(args[1]);
            }
            if (args.length > 2 && args[2] != null) {
                extractors = Integer.parseInt(args[2]);
            }
            if (args.length > 3 && args[3] != null) {
                perHost = Integer.parseInt(args[3]);
            }
            try {
                Crawler crawler = new WebCrawler(new CachingDownloader(), downloaders, extractors, perHost);
                crawler.download(url, 2);
                crawler.close();
            } catch (MalformedURLException e) {
                System.out.println("Bad url");
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            System.out.println("Wrong arguments.\n Usage: WebCrawler url [downloads [extractors [perHost]]]");
        }
    }

    /**
     * Downloads web pages from given url, extracts links from document and download them recursively
     *
     * @param url   web page to start from
     * @param depth recursion depth
     * @return visited web pages
     */
    @Override
    public Result download(String url, int depth) {

        ExecutionContext executionContext = new ExecutionContext(depth, downloaders, executors, perHost, downloader, hosts);
        Lock lock = executionContext.lock;
        lock.lock();
        try {
            executionContext.visited.add(url);
            executionContext.tasksNum.set(1);
            downloaders.submit(new DownloadWorker(url, 1, executionContext));
            try {
                while (executionContext.tasksNum.get() != 0) {
                    executionContext.condition.await();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } finally {
            lock.unlock();
            close();
        }
        executionContext.errors.keySet().forEach(executionContext.visited::remove);
        return new Result(executionContext.visited.stream().collect(Collectors.toList()), executionContext.errors);
    }

    /**
     * Closes all working threads
     */
    @Override
    public void close() {
        executors.shutdown();
        downloaders.shutdown();
    }
}

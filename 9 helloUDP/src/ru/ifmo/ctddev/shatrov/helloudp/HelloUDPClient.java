package ru.ifmo.ctddev.shatrov.helloudp;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by vi34 on 28.04.15.
 */
public class HelloUDPClient implements HelloClient {
    private ExecutorService executorService;
    private InetSocketAddress address;
    private int queries;

    public HelloUDPClient() {

    }

    public static void main(String[] args) {
        if (args != null && args.length > 4 && args[0] != null && args[1] != null && args[2] != null
                && args[3] != null && args[4] != null) {
            HelloUDPClient client = new HelloUDPClient();
            client.start(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[4]), Integer.parseInt(args[3]));
        } else {
            System.err.println("Wrong arguments!\n Usage: ip port prefix threads queries");
        }
    }

    @Override
    public void start(String ip, int port, String prefix, int queries, int threads) {
        executorService = Executors.newFixedThreadPool(threads);
        this.queries = queries;
        try {
            this.address = new InetSocketAddress(InetAddress.getByName(ip), port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        List<Future> futures = new ArrayList<>();
        for (int i = 0; i < threads; ++i) {
            futures.add(executorService.submit(new Worker(prefix, i)));
        }
        for (Future f : futures) {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
        }
        executorService.shutdown();
    }

    private class Worker implements Runnable {
        private String prefix;
        private int workerId;

        Worker(String prefix, int id) {
            this.prefix = prefix;
            this.workerId = id;
        }

        @Override
        public void run() {
            try (DatagramSocket socket = new DatagramSocket()) {
                String message, expected;
                for (int i = 0; i < queries; ++i) {
                    message = prefix + workerId + "_" + i;
                    expected = "Hello, " + message;
                    byte[] mBytes = message.getBytes("UTF-8");
                    DatagramPacket packetSend = new DatagramPacket(mBytes, mBytes.length, address.getAddress(), address.getPort());

                    while (true) {
                        try {
                            socket.send(packetSend);
                            System.out.println(message);
                            socket.setSoTimeout(100);
                            try {
                                byte[] rBytes = new byte[socket.getReceiveBufferSize()];
                                DatagramPacket packetReceive = new DatagramPacket(rBytes, rBytes.length);
                                socket.receive(packetReceive);
                                String rec = new String(packetReceive.getData(),packetReceive.getOffset(), packetReceive.getLength(), "UTF-8");
                                if (rec.equals(expected)) {
                                    System.out.println(rec);
                                    break;
                                }
                            } catch (SocketTimeoutException ignored) {}

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (SocketException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }
}

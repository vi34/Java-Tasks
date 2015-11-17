package ru.ifmo.ctddev.shatrov.helloudp;

import info.kgeorgiy.java.advanced.hello.HelloServer;
import javafx.util.Pair;

import javax.swing.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by vi34 on 29.04.15.
 */
public class HelloUDPServer implements HelloServer {
    private ConcurrentLinkedQueue<Pair<DatagramSocket, List<Thread>>> executionList = new ConcurrentLinkedQueue<>();
    private static final String RESPONSE = "Hello, ";
    private static byte[] RESPONSE_BYTES;
    private static int RESPONSE_LENGTH;

    public HelloUDPServer() {
        try {
            RESPONSE_BYTES = RESPONSE.getBytes("UTF-8");
            RESPONSE_LENGTH = RESPONSE_BYTES.length;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args != null && args.length > 1 && args[0] != null && args[1] != null) {
            HelloUDPServer server = new HelloUDPServer();
            server.start(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        } else {
            System.err.println("Wrong arguments\n Usage: port threads");
        }
    }

    @Override
    public void start(int port, int threads) {

        try {
            DatagramSocket socket = new DatagramSocket(port);
            socket.setSoTimeout(100);
            List<Thread> threadList = new ArrayList<>();
            for (int i = 0; i < threads; ++i) {
                Thread thread = new Thread(new Worker(socket));
                threadList.add(thread);
                thread.start();
            }
            executionList.add(new Pair<>(socket, threadList));
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        for (Pair<DatagramSocket, List<Thread>> pair : executionList) {
            pair.getValue().forEach(java.lang.Thread::interrupt);
        }
        for (Pair<DatagramSocket, List<Thread>> pair : executionList) {
            for (Thread thread : pair.getValue()) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            pair.getKey().close();
        }
    }

    private class Worker implements Runnable {
        private DatagramSocket receiveSocket;

        private Worker(DatagramSocket socket) {
            this.receiveSocket = socket;
        }

        @Override
        public void run() {
            try (DatagramSocket sendSocket = new DatagramSocket()) {
                byte[] buf = new byte[receiveSocket.getReceiveBufferSize()];
                System.arraycopy(RESPONSE_BYTES, 0, buf, 0, RESPONSE_LENGTH);
                DatagramPacket packet = new DatagramPacket(buf, RESPONSE_LENGTH, buf.length - RESPONSE_LENGTH);
                while (!Thread.interrupted()) {
                    packet.setData(buf, RESPONSE_LENGTH, buf.length - RESPONSE_LENGTH);
                    try {
                        receiveSocket.receive(packet);
                    } catch (SocketTimeoutException e) {
                        continue;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    packet.setData(buf, 0, packet.getLength() + RESPONSE_LENGTH);
                    try {
                        sendSocket.send(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
    }
}
